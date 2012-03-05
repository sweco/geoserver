var textarea = document.getElementById('$componentId');
var editor = CodeMirror.fromTextArea(textarea, { 
    $options,
    onBlur: function(e) {
        if ($onBlurCallbackURL) {
          wicketAjaxPost($onBlurCallbackURL, e.getValue());
        }
    }
});
if(!document.gsEditors) {
    document.gsEditors = {};
}
document.gsEditors.$componentId = editor;
document.getElementById('cm_undo').onclick = function() {
    editor.undo();
};
document.getElementById('cm_redo').onclick = function() {
    editor.redo();
};
document.getElementById('cm_goto').onclick = function() {
    var line = Number(prompt("Jump to line:", ""));
    var last = editor.lineCount();
    line = (line <= 1) ? 1 : (line > last) ? last : line;
    line = line - 1;
    
    if (!isNaN(line)) {
      editor.setCursor(line,0);
      editor.setSelection({line:line,ch:0},{line:line+1,ch:0});
      editor.focus();
    }
};
document.getElementById('cm_font_size').onchange = function() {
    var fontSize = document.getElementById('cm_font_size').value;
    editor.getScrollerElement().style.fontSize = fontSize + "px";
}

//JD: no longer really needed with smart indent
/*document.getElementById('cm_reformat').onclick = function() {
    if(editor.selection()) {
        editor.reindentSelection();
    } else {
        editor.reindent();
    }   
}*/
// This comes from http://thereisamoduleforthat.com/content/making-div-fullscreen-and-restoring-it-its-original-position
// Does not work so commented out
/*
document.getElementById('cm_fullscreen').onclick = function() {
    div = $('#$container');
    if (!div.hasClass('fullscreen')) { // Going fullscreen:
        alert("Sigh, can't make this work at all...");
      // Save current values.
      editor.beforeFullscreen = {
        parentElement: div.parent(),
        index: div.parent().children().index(div),
        x: $(window).scrollLeft(), y: $(window).scrollTop(),
      };

      // Set values needed to go fullscreen.
      $('body').append(div).css('overflow', 'hidden');
      div.addClass('fullscreen');
      window.scroll(0,0);
    } else { // Going back to normal:
      // Restore saved values.
      div.removeClass('fullscreen');
      if (editor.beforeFullscreen.index >= editor.beforeFullscreen.parentElement.children().length) {
          editor.beforeFullscreen.parentElement.append(div);
      } else {
          div.insertBefore(editor.beforeFullScreen.parentElement.children().get(editor.beforeFullscreen.index));
      }
      $('body').css('overflow', 'auto');
      window.scroll(editor.beforeFullscreen.x, editor.beforeFullscreen.y);
      editor.beforeFullScreen = null;
    }
  };
*/