$(document).ready(function() {
  $('[data-toggle="popover"]').popover({
    trigger: "hover",
    html: true,
    content: function() {
      return '<img class="result-image" src="' + $(this).data("img") + '"/>';
    }
  });
});
