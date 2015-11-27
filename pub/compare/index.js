(function() {

var POS = [ "NNP", "NNS", "VBD", "VBG", "VBN", "VBP", "NNPS", "VB", "VBZ", "RB", "JJ", "NN" ];

var $e = function(name) {
  return $(document.createElement(name));
};

var loadIndex = function() {
  var $content = $('#content')
    .text('')
    .addClass('index')
    .removeClass('book');

  $e('h1').addClass('title')
    .text('Books')
    .appendTo($content);

  $.getJSON('/index.json', function(data) {
    data.forEach(function(book) {
      $e('div').addClass('book')
        .append($e('a').text(book.name_b + ' (' + book.name_a + ')')
                  .attr('href', '#' + book.abbr))
        .appendTo($content);
    });
  });
};

var loadBook = function(name) {
  var $content = $('#content')
    .text('')
    .addClass('book')
    .removeClass('index')
    .on('mouseover', function(event) {
      var $tgt = $(event.target);
      if (!$tgt.hasClass('tok')) {
        return;
      }

      $('.tok.active').removeClass('active');

      var tid = $tgt.attr('id'),
          suf = tid.substring(1),
          oid = (tid.charAt(0) == 'r') ? 'l' + suf : 'r' + suf;

      $tgt.addClass('active');
      $('#' + oid).addClass('active');
    }).on('mouseout', function(event) {
      var $tgt = $(event.target);
      if (!$tgt.hasClass('tok')) {
        return;
      }
      $('.tok.active').removeClass('active');
    }).on('click', function(event) {
      var $tgt = $(event.target);
      if (!$tgt.hasClass('tok')) {
        return;
      }
      console.log($tgt.attr('data-pos'));
    });

  $.getJSON('/' + name + '.json', function(data) {
    $e('div').addClass('name')
      .addClass('left')
      .text(data.name_a)
      .appendTo($content);

    $e('div').addClass('name')
      .addClass('right')
      .text(data.name_b)
      .appendTo($content);

    data.verses.forEach(function(verse) {
      var $verse = $e('div').addClass('verse'),
          $left = $e('div').addClass('left').appendTo($verse)
          $rite = $e('div').addClass('right').appendTo($verse),
          vid = verse.num.replace('.', '-');

      $e('span').addClass('num')
        .text(verse.num)
        .appendTo($left);

      $e('span').addClass('num')
        .text(verse.num)
        .appendTo($rite);

      var ta = verse.text_a,
          tb = verse.text_b;
      for (var i = 0, n = ta.length; i < n; i++) {
        var cla = (ta[i].text == tb[i].text) ? 'same' : 'diff',
            clb = (POS.indexOf(ta[i].pos) >= 0) ? 'repl' : 'none';

        $e('span').addClass('tok')
          .addClass(cla)
          .addClass(clb)
          .attr('id', 'l' + vid + '-' + i)
          .attr('data-pos', ta[i].pos)
          .text(ta[i].text)
          .appendTo($left);

        $e('span').addClass('tok')
          .addClass(cla)
          .addClass(clb)
          .attr('id', 'r' + vid + '-' + i)
          .attr('data-pos', tb[i].pos)
          .text(tb[i].text)
          .appendTo($rite);
      }

      $verse.appendTo($content);
    });
  });
};

var load = function() {
  var url = location.hash.substring(1);
  if (url == '') {
    loadIndex();
  } else {
    loadBook(url);
  }
};

window.onpopstate = load;

load();

})();
