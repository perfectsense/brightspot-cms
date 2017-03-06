define([
    'jquery',
    'jquery.extra',
    'input/leaflet' ],

function($) {
    $.plugin2('locationMap', {
        '_create': function(locationMap) {
            var plugin = this;

            var map = L.map(locationMap, {
                    scrollWheelZoom: false
            });
            new L.TileLayer.OpenStreetMap().addTo(map);

            var latInput = $(locationMap).find(".locationMapLatitude");
            var longInput = $(locationMap).find(".locationMapLongitude");
            var zoomInput = $(locationMap).find(".locationMapZoom");

            var lat = $(latInput).val();
            var lng = $(longInput).val();
            var zoom = $(zoomInput).val();

            if ((!lat || lat === '' || lat.length === 0) ||
                (!lng || lng === '' || lng.length === 0)) {
                lat = 39.8282;
                lng = -98.5795;
                zoom = 4;
            }

            map.setView([lat, lng], zoom);

            map.on('zoomend', function(e) {
                zoomInput.val(map.getZoom());
            });

            // Add a location marker to the map
            var marker = L.marker([lat, lng], { draggable: true}).addTo(map);
            marker.on('dragend', function(e) {
                plugin.updateLatLng(e.target, map, latInput, longInput, zoomInput);
            });

            // Update the marker's location with a new lat/long value
            var updateMarkerLocation = function(latitude, longitude) {
                if (!map.hasLayer(marker)) {
                    marker.addTo(map);
                }
                marker.setLatLng([latitude, longitude]);
                plugin.updateLatLng(marker, map, latInput, longInput, zoomInput);
            };

            // --- Geocoder Address Search

            new L.Control.GeoSearch({
                zoomLevel: 16,
                showMarker: false,
                provider: new L.GeoSearch.Provider.OpenStreetMap()
            }).addTo(map);

            // Update marker location after a geo search result is found
            map.on("geosearch_foundlocations", function(event) {
                if (event.Locations && event.Locations.length > 0) {
                    updateMarkerLocation(event.Locations[0].Y, event.Locations[0].X);
                }
            });

            // Prevent the enter key in the address search from submitting the CMS page form
            $("#leaflet-control-geosearch-qry").keypress(function(event) {
                if (event.which === 13) {
                    event.preventDefault();
                }
            });

            // --- Current Location

            // Add a button to mark to user's current location, using HTML5 geolocation
            L.control.locate({
                drawCircle: false,
                icon: 'icon icon-map-marker',
                iconLoading: 'icon icon-rss',
                strings: {
                    title: "Set marker to your current location",
                    popup: "<b>Current location</b><br/>(within {distance} {unit})"
                }
            }).addTo(map);

            // Update the location marker when we find the user's current location
            map.on("locationfound", function(event) {
                updateMarkerLocation(event.latitude, event.longitude);
            });
        },

        'updateLatLng' : function(marker, map, latInput, longInput, zoomInput) {
            var latlong = marker.getLatLng();
            latInput.val(latlong.lat);
            longInput.val(latlong.lng);
            zoomInput.val(map.getZoom());
        }
    });
});

