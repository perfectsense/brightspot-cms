define([ 'jquery' ], function($) {
  $(document).on('frame-load', '.frame.taxonChildren', function() {
    $(this).closest('.taxonomyContainer').scrollLeft(30000);
  });
});