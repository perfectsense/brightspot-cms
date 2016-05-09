define([ 'jquery' ], function($) {
  $(document).on('frame-load', '.searchResultTaxonomyChildren', function() {
    $(this).closest('.searchResultTaxonomy').scrollLeft(30000);
  });
});