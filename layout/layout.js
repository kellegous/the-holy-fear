var ipc = require('ipc'),
    $ = require('./jquery');

(function() {

var toRoman = function(num) {
  var rom = ["M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"],
      dec = [1000,900,500,400,100,90,50,40,10,9,5,4,1],
      res = '';
  if (num <= 0 || num >= 4000) {
    return num;
  }

  for (var i = 0, n = rom.length; i < n; i++) {
    while (num >= dec[i]) {
      num -= dec[i];
      res += rom[i];
    }
  }
  return res;
};

var $e = function(name) {
  return $(document.createElement(name));
};

var RightMostColumn = function(el) {
  return Math.max(el.getClientRects().map(function(r) {
    return r.left;
  }));
};

var State = function($cnt, w, h) {
  this.w = w;
  this.h = h;
  this.$cnt = $cnt.text('').css('opacity', 1);
};

State.prototype.hasOverflowed = function() {
  return this.$pag.get(0).getBoundingClientRect().height > this.h;
};

State.prototype.newPage = function() {
  this.$pag = $e('div').addClass('page')
    .appendTo(this.$cnt);
}

State.prototype.newCols = function() {
  this.$col = $e('div').addClass('cols')
    .appendTo(this.$pag);
}

State.prototype.addBookName = function(name) {
  this.newPage();
  var $el = $e('h2').text(name)
    .appendTo(this.$pag);
  this.newCols();
}

State.prototype.addChapter = function(number) {
  var $el = $e('span').addClass('chapter-num')
    .text(number)
    .appendTo(this.$col);

  if (!this.hasOverflowed()) {
    return;
  }

  $el.remove();
  this.newPage();
  this.newCols();
  this.addChapter(number);
};

State.prototype.addChapterEnd = function() {
  $e('div').addClass('chapter-end')
    .appendTo(this.$col);
};

State.prototype.fixChapterNums = function() {
  this.$pag.find('.chapter-num').each(function(i, e) {
    var p = e.previousElementSibling;
    if (!p) {
      return;
    }

    if (e.getBoundingClientRect().left - p.getBoundingClientRect().left > 50) {
      $(p).css('-webkit-column-break-after', 'always')
        .css('-moz-column-break-after', 'always');
    }
  });
};

State.prototype.addVerseNum = function(number) {
  var $el = $e('span').addClass('verse-num')
    .text(number)
    .appendTo(this.$col);
  if (!this.hasOverflowed()) {
    return;
  }

  $el.remove();
  this.newPage();
  this.newCols();
  this.addVerseNum(number);
};

State.prototype.addVerseTxt = function(text, id) {
  var $el = $e('span').addClass('verse-txt')
    .addClass(id)
    .text(text)
    .appendTo(this.$col);
  if (!this.hasOverflowed()) {
    return;
  }

  $el.remove();
  this.fixChapterNums();
  // if (this.hasOverflowed()) {
  //   throw new Error('need to backtrack');
  // }

  var words = text.split(' '),
      stuff = [];
  while (words.length > 0) {
    stuff.unshift(words.pop());
    $el = $e('span').addClass('verse-txt')
      .addClass(id)
      .text(words.join(' '))
      .appendTo(this.$col);
    if (!this.hasOverflowed()) {
      break;
    }
    $el.remove();
  }

  this.newPage();
  this.newCols();
  this.addVerseTxt(stuff.join(' '), id);
};

var Render = function(books, whenDone) {
  var state = new State($('#content').text('').css('opacity', 1), 800, 1000);

  books.forEach(function(book) {
    state.addBookName(book.name);
    state.newCols();

    book.chapters.forEach(function(chapter) {
      state.addChapter(chapter.num);
      chapter.verses.forEach(function(verse) {
        state.addVerseNum(verse.num);
        state.addVerseTxt(
          verse.txt,
          book.abbr + '-' + chapter.num + '-' + verse.num);
      });
      state.addChapterEnd();
    });
  });

  whenDone($('.page').toArray());
};

$('#content').text('Fuck You Web').css('opacity', 0)
  .append($e('h2').text('Hard!'));

ipc.on('begin-layout', function(data) {
  setTimeout(function() {
    Render(data, function(pages) {
      pages.forEach(function(page, ix) {
        ipc.send('save-file', ix, page.innerHTML);
      });
      ipc.send('quit');
    });
  }, 1000);
});

})();