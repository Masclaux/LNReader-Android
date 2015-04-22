// body tag get in initGesture()
var myTag;

//Save previous scale
var lastScale      = 1.0;

//scale
var currentScale   = 1.0;

//original url
var originalUrl    = "";

//OnLoad initializer
function initGesture( scale )
{
   originalUrl = document.location.href;

   console.log("GESTURE_JS: Init with scale : " + scale);

   lastScale    = scale;
   currentScale = scale;

    myTag = document.getElementsByTagName("body")[0];

    //delete un-necessary handlers
    delete Hammer.defaults.cssProps.userSelect;

    // create instance
    var mc = new Hammer( myTag,{ touchAction: 'pan-y' });

    //add pinch recognition
    var pinch = new Hammer.Pinch();
    mc.add(pinch);

    console.log("GESTURE_JS: add events");

    // listeners
   mc.on("pinchin", pinchIn);
   mc.on("pinchout",pinchOut);
   mc.on("pinchend",pinchEnd);

   resizeText();
}


//return scale to android
function getScale()
{
    console.log("GESTURE_JS: send scale" + lastScale);
    document.location  = document.location +  "/#scale=" + currentScale;
}

function pinchIn(ev)
{
    currentScale = Math.max(1, Math.min(lastScale * ev.scale, 10));
}

function pinchOut(ev)
{
    currentScale = Math.max(1, Math.min(lastScale * ev.scale, 10));
}

function pinchEnd(ev)
{
    lastScale = Math.max(1, Math.min(lastScale * ev.scale, 10));
    currentScale = lastScale;

    console.log("GESTURE_JS: pinch end " + lastScale);

    //send scale to android
    getScale();
}

//Resize text with current scale
function resizeText()
{
  if (document.body.style.fontSize == "")
  {
    document.body.style.fontSize = "1.0em";
  }

  document.body.style.fontSize =   currentScale + "em";

  //send scale to android
  getScale();

  console.log("GESTURE_JS : set scaling " + currentScale);
}