(function() {

var $e = function(name) {
  return $(document.createElement(name));
};

var render = function(pdf, pageNum, cb) {
  var cnt = document.querySelector('#content'),
      div = document.createElement('div'),
      cnv = document.createElement('canvas'),
      ctx = cnv.getContext('2d');

  cnv.width = 800;
  cnv.height = cnv.width * 1.294;

  div.classList.add('page');

  div.appendChild(cnv);
  cnt.appendChild(div);

  pdf.getPage(pageNum).then(function(page) {
    page.getTextContent().then(function(text) {
      console.log(text);
    });

    page.render({
      viewport: page.getViewport(1.33),
      canvasContext: ctx
    });

    cb();
  });
};

PDFJS.workerSrc = '/pdf.js/build/pdf.worker.js';
PDFJS.getDocument('fear.pdf').then(function(pdf) {
  console.log(pdf.numPages);
  render(pdf, 1, function() {
    render(pdf, 2, function() {
    });
  });
});

})();
