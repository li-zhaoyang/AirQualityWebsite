var CHINA_SOUTH_LAT = 3;
var CHINA_NORTH_LAT = 54;
var CHINA_WEST_LON = 72;
var CHINA_EAST_LON = 135;

var grayScale = L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'mapbox.streets',
    accessToken: 'pk.eyJ1IjoibGl6aGFveWFuZzA0MTYiLCJhIjoiZWE0YWYzNTNhZWZkYjI5M2QwNDJhOWFhZjM4NjQ1MDYifQ.JbnDIQcPsj9rtnqTL2FBqg'
});
var path = window.location.pathname;
var page = path.split("/").pop();
var maxZoom = 8;
var minZoom = 4;
var centerList = [35.76, 106.41];
var corner1 = L.latLng(CHINA_SOUTH_LAT, CHINA_WEST_LON),
corner2 = L.latLng(CHINA_NORTH_LAT, CHINA_EAST_LON),
corner3 = L.latLng(39, 115),
corner4 = L.latLng(42, 118),
bounds = L.latLngBounds(corner1, corner2);
if (page == "Beijing.html") {
  maxZoom = 13;
  minZoom = 9;
  centerList = [39.908657, 116.397212];
  bounds = L.latLngBounds(corner3, corner4);
}



Window.myMap = L.map('mapid',{
  center: centerList,
  zoom: minZoom,
  layers: [grayScale],
  minZoom: minZoom,
  maxZoom: maxZoom,
  maxBounds: bounds,
});
//
// var timeTextElement = document.getElementById("timeText");
// timeTextElement.innerHTML = "Latest";

var slider = document.getElementById("hourRangeInput");
slider.oninput = onSliderInput();
slider.addEventListener("input", onSliderInput);

function onSliderInput() {
    var slider = document.getElementById("hourRangeInput");
    var timeTextElement = document.getElementById("timeText");
    console.log(slider.value);

    function loadFile(filePath) {
      var result = null;
      var xmlhttp = new XMLHttpRequest();
      xmlhttp.open("GET", filePath, false);
      xmlhttp.send();
      if (xmlhttp.status==200) {
        result = xmlhttp.responseText;
      }
      return result;
    }
    var latestHour = loadFile("LatestHour.txt");
    latestHour = latestHour.substring(11);
    var latestHourInt = parseInt(latestHour);

    var hourStr = "";
    if (latestHourInt != parseInt(new Date().getUTCHours())) {
      var selectedDate = new Date(new Date().getTime() + (slider.value * 60 * 60 * 1000) + 7 * 60 * 60 * 1000);
    } else {
      var selectedDate = new Date(new Date().getTime() + (slider.value * 60 * 60 * 1000) + 8 * 60 * 60 * 1000);
    }

    if (selectedDate.getUTCHours() < 10) {
      hourStr = "0" + selectedDate.getUTCHours();
    } else {
      hourStr = "" + selectedDate.getUTCHours();
    }
    var dateString = selectedDate.getUTCFullYear() +"/"+ (selectedDate.getUTCMonth() + 1) +"/"+ selectedDate.getUTCDate() + " " + hourStr + ":00";
    timeTextElement.innerHTML = dateString;
    console.log(dateString);

    var selectedHour = latestHourInt + parseInt(slider.value)
    if (selectedHour < 0) {
      selectedHour += 24;
    }
    console.log(selectedHour)
    if (selectedHour < 10) {
      filePathHour = "0" + selectedHour;
    } else {
      filePathHour = "" + selectedHour;
    }
    console.log(filePathHour)

    if (Window.currentLayer != null) {
      Window.myMap.removeLayer(Window.currentLayer)
    }
    Window.currentLayer = L.tileLayer('tiles/' + filePathHour +'/{z}/{x}-{y}.png', {
        attribution: '',
        maxZoom: 13,
        id: 'dev',
    })
    Window.currentLayer.addTo(Window.myMap);
}
