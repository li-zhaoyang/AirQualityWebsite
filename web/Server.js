var http = require('http');
var fs = require('fs');
var url = require("url");

http.createServer(function (request, response) {
  var pathname = url.parse(request.url).pathname;
    console.log("Request for " + pathname + " received.");
    response.writeHead(200);
    if(pathname == "/") {
        html = fs.readFileSync("MyMap.html", "utf8");
        response.write(html);
    } else {
      try {
        response.write(fs.readFileSync(pathname.substring(1)))
      } catch (e) {
        console.trace();
      } finally {

      }
    }
    response.end();
}).listen(888);
