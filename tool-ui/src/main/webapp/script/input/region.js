define([
    'jquery',
    'jquery.extra',
    'input/leaflet' ],

function($) {
    $.plugin2('regionMap', {
        '_create': function(regionMap) {
            var plugin = this;

            var map = L.map(regionMap, {
                    scrollWheelZoom: false
            });
            new L.TileLayer.MapQuestOpenOSM().addTo(map);

            var zoomInput = $(regionMap).find(".regionMapZoom");
            var centerInput = $(regionMap).find(".regionMapCenter");
            var geoJsonInput = $(regionMap).find(".regionMapGeoJson");

            var geojson = $.parseJSON($(geoJsonInput).val());

            var myStyle = {
                "color": "#3a87ad",
                "opacity": 1,
                "weight": 4,
                "fillColor": "#3a87ad",
                "fillOpacity": 0.5
            };

            var zoom = zoomInput.val();
            var center = $.parseJSON(centerInput.val());

            map.setView([center.lat, center.lng], zoom);

            var regionLayer = new L.FeatureGroup();
            map.addLayer(regionLayer);

            // Leaflet/GeoJSON doesn't support Circles, so convert to Leaflet Points for geometryToLayer()
            if (geojson.geometries) {
                geojson.geometries.forEach(function (geometry) {
                    if (geometry.type == "Circle") {
                        geometry.type = "Point";

                        // Convert an array of coordinate arrays to Leaflet's format
                        if (Array.isArray(geometry.coordinates) && geometry.coordinates.length > 0 &&
                            Array.isArray(geometry.coordinates[0]) && geometry.coordinates[0].length > 1) {
                            geometry.coordinates = [geometry.coordinates[0][1], geometry.coordinates[0][0]];
                        }
                    }
                });
            }

            var savedItems = L.GeoJSON.geometryToLayer(geojson, function(feature, latlng) {
                // If a point has a radius, draw as a Circle
                if (feature.geometry && feature.geometry.type == "Point" && feature.geometry.radius > 0) {
                    return new L.Circle(latlng, feature.geometry.radius);
                }
                else {
                    return new L.Marker(latlng)
                }
            });

            for (x in savedItems.getLayers()) {
                var layer = savedItems.getLayers()[x];

                if (layer instanceof L.MultiPolygon) {
                    // Convert to individual polygons because leaflet.draw does
                    // not understand MultiPolygon.
                    var latlngs = layer.getLatLngs();
                    for (x in latlngs) {
                        var polygon = new L.Polygon(latlngs[x], { shapeOptions: myStyle });
                        regionLayer.addLayer(polygon);
                    }
                } else {
                    if (layer.setStyle) {
                        layer.setStyle(myStyle);
                    }
                    regionLayer.addLayer(layer);
                }
            }
            regionLayer.setStyle(myStyle);

            var drawControl = new L.Control.Draw({
                draw: {
                    polyline:  false,
                    rectangle: false,
                    marker:    false,
                    polygon:   { allowIntersection: false, shapeOptions: myStyle },
                    circle:    { shapeOptions: myStyle }
                },
                edit: {
                    featureGroup: regionLayer,
                    remove: true
                }
            });
            map.addControl(drawControl);

            // Add an address search using the OSM geocoder
            new L.Control.GeoSearch({
                zoomLevel: 16,
                provider: new L.GeoSearch.Provider.OpenStreetMap()
            }).addTo(map);

            // Prevent the enter key in the address search from submitting the CMS page form
            $("#leaflet-control-geosearch-qry").keypress(function(event) {
                if (event.which === 13) {
                    event.preventDefault();
                }
            });

            // Add a button to mark to user's current location, using HTML5 geolocation
            L.control.locate({
                drawCircle: false,
                markerClass: L.marker,
                icon: 'icon icon-map-marker',
                iconLoading: 'icon icon-rss',
                strings: {
                    title: "Set marker to your current location",
                    popup: "<b>Current location</b><br/>(within {distance} {unit})"
                }
            }).addTo(map);

            // Add custom circle JSON serialization since GeoJSON doesn't support circles
            var circleToGeoJSON = L.Circle.prototype.toGeoJSON;
            L.Circle.include({
                toGeoJSON: function() {
                    var feature = circleToGeoJSON.call(this);
                    feature.geometry = {
                        type: 'Circle',
                        coordinates: [[this.getLatLng().lat, this.getLatLng().lng]],
                        radius: this.getRadius()
                    };
                    return feature;
                }
            });

            // Bind map events.

            var updateFunc = function (e) {
                var geojson = regionLayer.toGeoJSON();
                geoJsonInput.val(JSON.stringify(geojson));
            };

            map.on('dragend', function(e) {
                zoomInput.val(map.getZoom());
                centerInput.val(JSON.stringify(map.getCenter()));
            });

            map.on('zoomend', function(e) {
                zoomInput.val(map.getZoom());
                centerInput.val(JSON.stringify(map.getCenter()));
            });

            map.on('draw:created', function (e) {
                regionLayer.addLayer(e.layer);
                updateFunc(e);
            });

            map.on('draw:edited', updateFunc);
            map.on('draw:deleted', updateFunc);
        }
    });
});
