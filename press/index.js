var ipc = require('ipc');

PDFJS.workerSrc = 'pdf.worker.js';

var Pad = function(val, len, pad) {
  val += '';
  pad = pad === undefined ? ' ' : pad;
  while (val.length < len) {
    val = pad + val;
  }
  return val;
};

var ExportPage = function(pdf, num, cb) {
  pdf.getPage(num).then(function(page) {
    page.getTextContent({normalizeWhitespace: true}).then(function(text) {
      cb(num, text, page.view);
    });
  });
};

ipc.on('export', function(url) {
  PDFJS.getDocument(url).then(function(pdf) {
    var last = pdf.numPages,
        next = function(num, text, view) {

          var styles = text.styles;
          text.items.forEach(function(item) {
            item.fontFamily = styles[item.fontName].fontFamily;
          });

          var data = {
            text : text.items,
            view: view
          };

          ipc.send('save-file',
            'text-' + Pad(num-1, 4, '0') + '.json',
            JSON.stringify(data));
          if (num == last) {
            ipc.send('done');
            return;
          }
          ExportPage(pdf, num + 1, next);
        };
    ExportPage(pdf, 1, next);
  });
});
