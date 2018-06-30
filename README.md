# iCrafted CrossChat Plugin for Sponge
The iCrafted CrossChat plugin for Sponge is a alternative way to setup a chat accross multiple servers without BungeeCord. Because we use simple sockets you are able too link every other type of software to the server and you can also supply your own server to use the CrossChat plugin against!

Too set it up you need to have a socket server ready that can send and receive messages from the connected clients, you can use our sample as shown below.
When you first start the plugin it will create you a configuration file where you need to enter the hostname and port (defaults to localhost:1337) so if you run in locally there is no issue.

Questions? Please create a ISSUE on our GitHub project page

## Setup a server
In order to use this CrossChat plugin you need to setup a NodeJS Socket server, you can use the example script as below

```javascript
/*
 * iCrafted CrossChat Server
 * @author iCrafted <hello@icrafted.eu>
 * @website https://www.icrafted.eu
 */

var port = 1337;

var net = require('net');

var clients = [];

var server = net.createServer(function(socket) {
    socket.name = socket.remoteAddress + ":" + socket.remotePort;

    clients.push(socket);

    socket.write("Welcome " + socket.name + "\n");
    broadcast(socket.name + " joined the chat\n", socket);

    socket.on('data', function(data) {
            for(var i in clients) {
                if(clients[i] === socket) continue;
                clients[i].write(data);
            }
    });

    socket.on('end', function () {
        clients.splice(clients.indexOf(socket), 1);
        broadcast(socket.name + " left the chat.\n");
    });

    // Send a message to all clients
    function broadcast(message, sender) {
        clients.forEach(function (client) {
        // Don't want to send it to sender
        if (client === sender) return;
            client.write(message);
        });
        // Log it to the server output too
        process.stdout.write(message)
    }

}).listen(port);

console.log("CrossChat server running at port " + port + "\n");
```
