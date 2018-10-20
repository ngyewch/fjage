const TIMEOUT = 5000;
const DEBUG = true;

export const Performative = {
  REQUEST: "REQUEST",               // Request an action to be performed.
  AGREE: "AGREE",                   // Agree to performing the requested action.
  REFUSE: "REFUSE",                 // Refuse to perform the requested action.
  FAILURE: "FAILURE",               // Notification of failure to perform a requested or agreed action.
  INFORM: "INFORM",                 // Notification of an event.
  CONFIRM: "CONFIRM",               // Confirm that the answer to a query is true.
  DISCONFIRM: "DISCONFIRM",         // Confirm that the answer to a query is false.
  QUERY_IF: "QUERY_IF",             // Query if some statement is true or false.
  NOT_UNDERSTOOD: "NOT_UNDERSTOOD", // Notification that a message was not understood.
  CFP: "CFP",                       // Call for proposal.
  PROPOSE: "PROPOSE",               // Response for CFP.
  CANCEL: "CANCEL"                  // Cancel pending request.
}

function _guid(len) {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
  }
  let s = s4();
  for (var i = 0; i < len-1; i++)
    s += s4();
  return s;
}

export class AgentID {

  constructor(name, topic, gw) {
    this.name = name;
    this.topic = topic;
    this.gw = gw;
  }

  getName() {
    return this.name;
  }

  isTopic() {
    return this.topic;
  }

  toString() {
    if (this.topic) return "#"+this.name;
    return this.name;
  }

  toJSON() {
    return this.toString();
  }

}

export class Message {

  constructor() {
    this.__clazz__ = "org.arl.fjage.Message";
    this.msgID = _guid(8);
  }

  static deserialize(obj) {
    if (typeof obj == 'string' || obj instanceof String) obj = JSON.parse(obj);
    let clazz = obj.clazz;
    clazz = clazz.replace(/^.*\./, "");
    let rv = eval("new "+clazz+"()");
    for (var key in obj.data)
      rv[key] = obj.data[key];
    return rv;
  }

  serialize() {
    let clazz = this.__clazz__;
    let data = JSON.stringify(this, (k,v) => {
      if (k.startsWith("__")) return undefined;
      return v;
    });
    return '{ "clazz": "'+clazz+'", "data": '+data+' }';
  }

}

export class GenericMessage extends Message {
  // TODO
}

export class Gateway {

  constructor() {
    let pending = {};
    let aid = "WebGW-"+_guid(4);
    let subscriptions = {};
    let listener = {};
    let queue = [];
    let sock = new WebSocket("ws://"+window.location.hostname+":"+window.location.port+"/ws/");
    sock.onopen = (event) => {
      sock.send("{'alive': true}\n");
      if ("onOpen" in pending) {
        pending.onOpen();
        delete pending.onOpen;
      }
    };
    sock.onmessage = (event) => {
      let obj = JSON.parse(event.data);
      if (DEBUG) console.log("< "+event.data);
      if ("id" in obj) {
        let id = obj.id;
        if (id in pending) {
          pending[id](obj);
        }
      } else if (obj.action == "send") {
        let msg = Message.deserialize(obj.message);
        if (msg.recipient == aid || subscriptions[msg.recipient]) {
          console.log(msg);
          queue.push(msg);
          for (var key in listener) {
            let cb = listener[key];
            if (cb(msg)) break;
          }
        }
      }
      // TODO
    };
    this.sock = sock;
    this.pending = pending;
    this.aid = aid;
    this.subscriptions = subscriptions;
    this.listener = listener;
    this.queue = queue;
  }

  _send(s) {
    let sock = this.sock;
    if (sock.readyState == sock.OPEN) {
      if (DEBUG) console.log("> "+s);
      sock.send(s+"\n");
      return true;
    } else if (sock.readyState == sock.CONNECTING) {
      this.pending.onOpen = () => {
        if (DEBUG) console.log("> "+s);
        sock.send(s+"\n");
      };
      return true;
    }
    return false;
  }

  _do(rq) {
    rq.id = _guid(8);
    let sock = this.sock;
    let pending = this.pending;
    return new Promise((resolve, reject) => {
      let timer = setTimeout(() => {
        delete pending[rq.id];
        reject();
      }, TIMEOUT);
      pending[rq.id] = (rsp) => {
        clearTimeout(timer);
        resolve(rsp);
      };
      if (!_send(JSON.stringify(rq))) {
        clearTimeout(timer);
        delete pending[rq.id];
        reject();
      }
    });
  }

  _get(filter) {
    if (filter == undefined) {
      if (this.queue.length == 0) return undefined;
      return this.queue.shift();
    }
    if (typeof filter == 'string' || filter instanceof String) {
      for (var i = 0; i < this.queue.length; i++) {
        let msg = this.queue[i];
        if ("inReplyTo" in msg && msg.inReplyTo == filter) {
          delete this.queue[i];
          return msg;
        }
      }
    }
    for (var i = 0; i < this.queue.length; i++) {
      let msg = this.queue[i];
      if (msg instanceof filter) {
        delete this.queue[i];
        return msg;
      }
    }
    return undefined;
  }

  import(name) {
    let sname = name.replace(/^.*\./, "");
    window[sname] = class extends Message {
      constructor() {
        super();
        this.__clazz__ = name;
      }
    };
  }

  getAgentID() {
    return this.aid;
  }

  agent(name) {
    return new AgentID(name, false, this);
  }

  topic(topic, topic2) {
    if (typeof topic == 'string' || topic instanceof String) return new AgentID(topic, true, this);
    if (topic2 == undefined) {
      if (topic instanceof AgentID) {
        if (topic.isTopic()) return topic;
        return new AgentID(topic.getName()+"__ntf", true, this);
      }
    } else {
      return new AgentID(topic.getName()+"__"+topic2+"__ntf", true, this)
    }
  }

  subscribe(topic) {
    if (!topic.isTopic()) topic = new AgentID(topic, true, this);
    this.subscriptions[topic.toString()] = true;
  }

  unsubscribe(topic) {
    if (!topic.isTopic()) topic = new AgentID(topic, true, this);
    delete this.subscriptions[topic.toString()];
  }

  async agentForService(service) {
    let rq = { action: 'agentForService', service: service };
    let rsp = await this._do(rq);
    return new AgentID(rsp.agentID, false, this);
  }

  async agentsForService(service) {
    let rq = { action: 'agentsForService', service: service };
    let rsp = await this._do(rq);
    let aids = [];
    for (var i = 0; i < rsp.agentIDs.length; i++)
      aids.push(new AgentID(rsp.agentIDs[i], false, this));
    return aids;
  }

  send(msg, relay=true) {
    msg.sender = this.aid;
    let s = '{ "action": "send", "relay": '+relay+', "message": '+msg.serialize()+' }';
    this._send(s);
  }

  async request(msg, timeout=10000) {
    this.send(msg);
    let rsp = await this.receive(msg.msgID, timeout);
    return rsp;
  }

  receive(filter=undefined, timeout=1000) {
    let queue = this.queue;
    let listener = this.listener;
    let self = this;
    return new Promise((resolve, reject) => {
      let msg = self._get(filter);
      if (msg != undefined) {
        resolve(msg);
        return;
      }
      let lid = _guid(8);
      let timer = setTimeout(() => {
        delete listener[lid];
        reject();
      }, timeout);
      listener[lid] = (msg) => {
        msg = self._get(filter);
        if (msg == undefined) return false;
        clearTimeout(timer);
        delete listener[lid];
        resolve(msg);
        return true;
      };
    });
  }

  close() {
    this.sock.send("{'alive': false}\n");
    this.sock.close();
  }

  shutdown() {
    this.close();
  }

}
