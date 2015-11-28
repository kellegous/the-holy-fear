var app = require('app'),
    BrowserWindow = require('browser-window'),
    fs = require('fs'),
    ipc = require('ipc'),
    path = require('path'),
    crypto = require('crypto'),
    argv = require('minimist')(process.argv.slice(2));

var Pad = function(val, len, pad) {
  val += '';
  pad = pad === undefined ? ' ' : pad;
  while (val.length < len) {
    val = pad + val;
  }
  return val;
};

var MakeDirsSync = function(dir) {
  var par = dir.split(path.sep);
  while (par.length > 0) {
    var cur = par.join(path.sep);
    if (fs.existsSync(cur)) {
      return;
    }

    fs.mkdirSync(cur);
    par.pop();
  }
};

if (argv._.length != 1) {
  process.exit(1);
}

argv.dst = argv.dst || 'out';

var buff = fs.readFileSync(argv._[0]),
    data = JSON.parse(buff),
    hash = crypto.createHash('sha1')
        .update(buff)
        .digest('hex')
        .substring(0, 8),
    dest = path.join(argv.dst, hash);

console.log(hash);

MakeDirsSync(dest);

app.on('ready', function() {
  var browser = new BrowserWindow({ width: 900, height: 900, show: false }),
      webContents = browser.webContents;
  browser.loadUrl('file://' + __dirname + '/layout.html');
  webContents.on('dom-ready', function() {
    webContents.send('begin-layout', data);
  });

  ipc.on('save-file', function(event, filename, contents) {
    fs.writeFileSync(path.join(dest, filename), contents);
  });

  ipc.on('quit', function(event) {
    // browser.show();
    process.exit(0);
  });
});
