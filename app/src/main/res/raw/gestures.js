// body tag get in initGesture()
var myBody;

//Save previous scale
var lastScale      = 1.0;

//scale
var currentScale   = 1.0;

//OnLoad initializer
function initGesture( scale )
{
    console.log("GESTURE_JS: Init with scale : " + scale);

   lastScale    = scale;
   currentScale = scale;

    myBody = document.getElementsByTagName("body")[0];

    // create instance
    var mc = new Hammer( myBody );

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
    console.log("RESIZE_TEXT_EVENT: send scale" + lastScale);
    BakaJS.setScale(lastScale);
}

function pinchIn(ev)
{
    var scale = Math.max(1, Math.min(lastScale * ev.scale, 10));
    resizeText(scale);
}

function pinchOut(ev)
{
    currentScale = Math.max(1, Math.min(lastScale * ev.scale, 10));
}

function pinchEnd(ev)
{
    lastScale = Math.max(1, Math.min(lastScale * ev.scale, 10));
    currentScale = lastScale;

    console.log("RESIZE_TEXT_EVENT: pinch end " + lastScale);
}

//Resize text with current scale
function resizeText()
{
  if (document.body.style.fontSize == "")
  {
    document.body.style.fontSize = "1.0em";
  }

  document.body.style.fontSize =   currentScale + "em";

  console.log("RESIZE_TEXT_EVENT : " + currentScale);
}