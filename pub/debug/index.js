(function() {

  var $e = function(name) {
    return $(document.createElement(name));
  };

  var Pad = function(val, len, pad) {
    val += '';
    pad = pad === undefined ? ' ' : pad;
    while (val.length < len) {
      val = pad + val;
    }
    return val;
  };

  var page = parseInt(location.hash.replace('#', '')) || 1;

  $.getJSON('/t' + Pad(page, 4, '0') + '.json', function(data) {
    var $cnt = $('#content');

    // $e('div').addClass('frame')
    //   .css('width', view[2] * fx)
    //   .css('height', view[3] * fx)
    //   .appendTo($cnt);

    data.forEach(function(item) {
      var $el = $e('div').addClass('text')
        .addClass('t' + item.type)
        .text(item.text)
        .css('left', item.l)
        .css('top', item.t)
        .css('width', item.w)
        // .css('height', item.h)
        .appendTo($cnt);

      if (item.tag) {
        $el.addClass(item.tag);
      }
    });
  });

})();
