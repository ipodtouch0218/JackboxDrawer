// ==UserScript==
// @name     JackboxDrawer
// @version  uwu~
// @include  *://jackbox.tv/*
// ==/UserScript==

//Catch outgoing messages and replace drawing data.
//Has to be done through eval to break through GreaseMonkey's sandboxing.
//Luckily, every single time data is sent, a message is logged. We override console.log and wait until
//the message "[Blobcast Client] send" is passed through, and then we can modify args[2], which contains
//the JSON data of the message we're about to upload to jackbox's servers.
window.eval(`
oldConsoleLog = console.log;
console.log = function(...args) {
  oldConsoleLog(args.join(' ')); //Send data to the existing console.log. We don't wanna lose out on any debug info.
  if (typeof(tempvar) == 'undefined' || tempvar == null) {
    //No custom code ready, most likely a vanilla subimssion. Ignore this one.
    return;
  }
  if (args[0] == '[Blobcast Client] send') {
    //Perform the switch-a-roo.
    //The specifics of the code that's ran depends on the game, and that's
    //all handled from the Java-app side of things. Check the source
    //code there for specifics.
    eval(tempvar);
    tempvar = null;
  }
}
`);

//Handle games and their differences.
const games = {
  "drawful_1": {
    submitDrawing: function() {
      document.getElementById("drawful-submitdrawing").click();
    },
    isInDrawingMode: function() {
      return !document.getElementsByClassName("state-draw")[0].getAttribute("class").includes("pt-page-off");
    },
    getSketchpad: function() {
      return document.getElementsByClassName("sketchpad")[0];
    }
  },
  "drawful_2": {
    submitDrawing: function() {
      document.getElementById("drawful-submitdrawing").click();
    },
    isInDrawingMode: function() {
      return !document.getElementsByClassName("state-draw")[0].getAttribute("class").includes("pt-page-off");
    },
    getSketchpad: function() {
      return document.getElementsByClassName("sketchpad")[0];
    }
  },
  "bidiots": {
    submitDrawing: function() {
      document.getElementById("auction-submitdrawing").click();
    },
    isInDrawingMode: function() {
      return !document.getElementById("state-draw").getAttribute("class").includes("pt-page-off");
    },
    getSketchpad: function() {
      return document.getElementById("auction-sketchpad");
    }
  },
  "tee_ko": {
    submitDrawing: function() {
      document.getElementById("awshirt-submitdrawing").click();
    },
    isInDrawingMode: function() {
      return !document.getElementById("state-draw").getAttribute("class").includes("pt-page-off");
    },
    getSketchpad: function() {
      return document.getElementsByClassName("awshirt-sketchpad")[0];
    }
  },
  "push_the_button": {
    submitDrawing: function() {
      document.getElementById("submitdrawing").click();
    },
    isInDrawingMode: function() {
      return document.getElementsByClassName("Draw")[0] == null;
    },
    getSketchpad: function() {
      return document.getElementById("fullLayer");
    }
  },
  "trivia_murder_party_1": {
    submitDrawing: function() {
      document.getElementById("enter-single-drawing-submit").click();
    },
    isInDrawingMode: function() {
      return !document.getElementById("state-enter-single-drawing").getAttribute("class").includes("pt-page-off");
    },
    getSketchpad: function() {
      return document.getElementById("sketchpad");
    }
  }
}
//Keeps track of the game we're currently playing.
var gameID = null;

function updateGame(id) {
  gameID = id;
  if (typeof(socket) != 'undefined' && socket != null) {
    //Update the drawing app on what game we're playing.
    socket.send("updategame:" + id); 
  }
}

//Is ran every time the document changes. Useful for finding which game we're currently playing.
const callback = function(mutationsList, observer) {
  if (document.getElementById("page-drawful") != null) {
    //Drawful 1 and 2 actually share the same ID, but have different graphcis modes.
    //Luckily, the drawing div has "drawful2-page" as a class in Drawful 2.
    if (document.getElementsByClassName("state-draw")[0].getAttribute("class").includes("drawful2-page")) {
      updateGame("drawful_2");
    } else {
      updateGame("drawful_1");
    }
  } else if (document.getElementById("page-auction") != null) {
    updateGame("bidiots");
  } else if (document.getElementById("page-awshirt") != null) {
    //Fun fact. Tee KO is actually internally called "awshirt" both on the website and in the game files.
    updateGame("tee_ko");
  } else if (document.getElementsByClassName("Push The Button")[0] != null) {
    //Yes, the class name has spaces.
    updateGame("push_the_button");
  } else if (document.getElementById("page-triviadeath") != null) {
    updateGame("trivia_murder_party_1");
  }
};

//Initiate the DOM observer to run "callback" every time it changes.
const observer = new MutationObserver(callback);
const targetNode = document.getElementById('content-region');
const config = { attributes: false, childList: true, subtree: true };
observer.observe(targetNode, config);

//Info related to communicating with the Java app.
var socket = null;
var open = false;
var firsttry = false;

//We want to automatically attempt reconnects if the connection is dropped, use setInterval with some
//checks to make sure we don't make multiple connections.
setInterval(function() {
  if (open || socket != null) {
    return;
  }
  socket = new WebSocket("ws://127.0.0.1:2460");
  
  socket.onopen = function(e) {
    alert("Connection established with JackboxDrawer program.");
    open = true;
    callback(null,null);
  };
  
  socket.onmessage = function(event) {
    //Save incoming code from the websocket in "tempvar". Needs to be eval'd to get through Greasemonkey's sandboxing.
    //We could also use window.wrappedJSObject but this is what I thought of first. Either way, potental security breach right here.
    window.eval("var tempvar = `" + event.data + "`");
    
    //Check to make sure we can actually DRAW right now.
    //If not, even attempting to submit a drawing can easily crash our webpage.
    
    if (typeof (games[gameID]) === 'undefined' || games[gameID] == null) {
    	alert("Game not supported!");
      return;
    }
    
    
    if (!games[gameID].isInDrawingMode()) {
      alert("Cannot submit drawing: Not in drawing mode!");
      window.eval("var tempvar = null;");
      return;
    }
    
    //Find the current sketchpad. We need it for later...
    let sketchpad = games[gameID].getSketchpad();
    if (sketchpad == null) {
      //Couldn't find it, we're probably can't draw right now. Somehow, the previous checks failed.
      return;
    }
    
    //Simulate drawing on the sketchpad with mouse events. We can't access the sketchpad's info directly
    //as it's kept track of internally, and the game never attempts to send any data if it's blank.
    let rect = sketchpad.getBoundingClientRect();
    let mouseEvent = document.createEvent('MouseEvents');
    
    mouseEvent.clientX = rect.x + rect.width / 2;
    mouseEvent.clientY = rect.y + rect.height / 2;
    mouseEvent.initEvent("mousedown", true, false);
    sketchpad.dispatchEvent(mouseEvent);
    mouseEvent.clientX += 2;
    mouseEvent.initEvent("mousemove", true, false);
    sketchpad.dispatchEvent(mouseEvent);
    mouseEvent.initEvent("mouseup", true, false);
    sketchpad.dispatchEvent(mouseEvent);
    
    //Submit drawing and get ready to switch-a-roo.
    games[gameID].submitDrawing();
  };
  
  //Socket died, my dude.
  socket.onclose = function(event) {
    if (!open) return;
    alert("Connection lost with JackboxDrawer program. Retrying...");
    open = false;
    socket = null;
  };
  
  //Socket died PAINFULLY, my dude.
  socket.onerror = function(error) {
    if (!firsttry) {
      alert("Failed to connect to JackboxDrawer. \nI will attempt to reconnect in the background...");
      firsttry = true;
    }
    socket = null;
  }
}, 1000);
