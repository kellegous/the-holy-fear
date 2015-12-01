var app = require('app'),
    BrowserWindow = require('browser-window'),
    fs = require('fs'),
    ipc = require('ipc'),
    path = require('path'),
    argv = require('minimist')(process.argv.slice(2));

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
  process.stderr.write('usage: electron press [options] file.pdf\n');
  process.exit(1);
}

argv.dst = argv.dst || 'dst/www';

var filename = '/' + path.relative('/', argv._[0]);

MakeDirsSync(argv.dst);

app.on('ready', function() {
  var browser = new BrowserWindow({ width: 900, height: 900, show: false }),
      webContents = browser.webContents;
  browser.loadUrl('file://' + __dirname + '/index.html');
  webContents.on('dom-ready', function() {
    webContents.send(
      'export',
      'file://' + filename);
  });

  ipc.on('save-file', function(event, filename, contents) {
    console.log(filename);
    fs.writeFileSync(path.join(argv.dst, filename), contents);
  });

  ipc.on('done', function() {
    process.exit(0);
  });
});
