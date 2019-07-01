/******************************************************************************

Copyright (c) 2015-2018, Mandar Chitre

This file is part of fjage which is released under Simplified BSD License.
See file LICENSE.txt or go to http://www.opensource.org/licenses/BSD-3-Clause
for full license details.

******************************************************************************/

package org.arl.fjage.remote;

import org.arl.fjage.AgentID;
import org.arl.fjage.connectors.Connector;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Handles a JSON/TCP connection with remote container.
 */
class ConnectionHandler extends Thread {

  private final String ALIVE = "{\"alive\": true}";
  private final String SIGN_OFF = "{\"alive\": false}";

  private Connector conn;
  private DataOutputStream out;
  private Map<String,Object> pending = Collections.synchronizedMap(new HashMap<String,Object>());
  private Logger log = Logger.getLogger(getClass().getName());
  private RemoteContainer container;
  private boolean alive, keepAlive;

  public ConnectionHandler(Connector conn, RemoteContainer container) {
    this.conn = conn;
    this.container = container;
    setName(conn.toString());
    alive = false;
    keepAlive = true;
  }

  @Override
  public void run() {
    ExecutorService pool = Executors.newSingleThreadExecutor();
    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    out = new DataOutputStream(conn.getOutputStream());
    if (keepAlive) println(ALIVE);
    while (conn != null) {
      String s = null;
      try {
        s = in.readLine();
      } catch(IOException ex) {
        // do nothing
      }
      if (s == null) break;
      log.fine(this.getName() +" <<< "+s);
      if (keepAlive) {
        // additional alive/sign-off logic needed on serial ports to avoid waiting for slaves when none present
        if (!alive) {
          alive = true;
          log.fine("Connection alive");
        } else if (s.equals(SIGN_OFF)) {
          alive = false;
          log.fine("Peer signed off");
          continue;
        }
        if (s.equals(ALIVE)) {
          if (container instanceof SlaveContainer) println(ALIVE);
          continue;
        }
      }
      // handle JSON messages
      try {
        JsonMessage rq = JsonMessage.fromJson(s);
        if (rq.action == null) {
          if (rq.id != null) {
            // response to some request
            Object obj = pending.get(rq.id);
            if (obj != null) {
              pending.put(rq.id, rq);
              synchronized(obj) {
                obj.notify();
              }
            }
          }
        } else {
          // new request
          pool.execute(new RemoteTask(rq));
        }
      } catch(Exception ex) {
        log.warning("Bad JSON request: "+ex.toString() + " in " + s);
      }
    }
    close();
    pool.shutdown();
  }

  private void respond(JsonMessage rq, boolean answer) {
    JsonMessage rsp = new JsonMessage();
    rsp.inResponseTo = rq.action;
    rsp.id = rq.id;
    rsp.answer = answer;
    println(rsp.toJson());
  }

  private void respond(JsonMessage rq, AgentID aid) {
    JsonMessage rsp = new JsonMessage();
    rsp.inResponseTo = rq.action;
    rsp.id = rq.id;
    rsp.agentID = aid;
    println(rsp.toJson());
  }

  private void respond(JsonMessage rq, AgentID[] aid) {
    JsonMessage rsp = new JsonMessage();
    rsp.inResponseTo = rq.action;
    rsp.id = rq.id;
    rsp.agentIDs = aid;
    println(rsp.toJson());
  }

  private void respond(JsonMessage rq, String[] svc) {
    JsonMessage rsp = new JsonMessage();
    rsp.inResponseTo = rq.action;
    rsp.id = rq.id;
    rsp.services = svc;
    println(rsp.toJson());
  }

  synchronized void println(String s) {
    if (out == null) return;
    try {
      int delay = (int)(Math.random() * 1000 - 500);
      if (delay < 0) delay = 0;
      try {
        Thread.sleep(delay);
      }catch(InterruptedException ex){}
      out.writeBytes(s+"\n");
      log.fine(this.getName() +" >>> "+s);
      conn.waitOutputCompletion(1000);
    } catch(IOException ex) {
      if (!s.equals(SIGN_OFF)) {
        log.warning("Write failed: "+ex.toString());
        close();
      }
    }
  }

  JsonMessage printlnAndGetResponse(String s, String id, long timeout) {
    if (conn == null) return null;
    if (keepAlive && !alive && container instanceof MasterContainer) return null;
    pending.put(id, id);
    try {
      synchronized(id) {
        println(s);
        id.wait(timeout);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    Object rv = pending.get(id);
    pending.remove(id);
    if (rv instanceof JsonMessage) return (JsonMessage)rv;
    if (keepAlive && alive) {
      alive = false;
      log.fine("Connection dead");
    }
    return null;
  }

  synchronized void close() {
    if (conn == null) return;
    if (keepAlive && container instanceof SlaveContainer) println(SIGN_OFF);
    conn.close();
    conn = null;
    out = null;
    container.connectionClosed(this);
  }

  boolean isClosed() {
    return conn == null;
  }

  //////// Private inner class representing task to run

  private class RemoteTask implements Runnable {

    private JsonMessage rq;

    RemoteTask(JsonMessage rq) {
      this.rq = rq;
    }

    @Override
    public void run() {
      switch (rq.action) {
        case AGENTS:
          respond(rq, container.getLocalAgents());
          break;
        case CONTAINS_AGENT:
          respond(rq, rq.agentID != null && container.containsAgent(rq.agentID));
          break;
        case SERVICES:
          respond(rq, container.getLocalServices());
          break;
        case AGENT_FOR_SERVICE:
          respond(rq, rq.service != null ? container.localAgentForService(rq.service) : null);
          break;
        case AGENTS_FOR_SERVICE:
          respond(rq, rq.service != null ? container.localAgentsForService(rq.service) : null);
          break;
        case SEND:
          if (rq.relay != null) container.send(rq.message, rq.relay);
          else container.send(rq.message);
          break;
        case SHUTDOWN:
          container.shutdown();
          break;
      }
    }

  } // inner class

}
