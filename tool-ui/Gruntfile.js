module.exports = function(grunt) {
    require('bsp-grunt')(grunt, {
        bsp: {
            bower: {
                'codemirror': [
                    {
                        dest: 'codemirror',
                        expand: true,
                        src: 'lib/**'
                    },

                    {
                        dest: 'codemirror',
                        expand: true,
                        src: [
                            'mode/clike/clike.js',
                            'mode/css/css.js',
                            'mode/htmlembedded/htmlembedded.js',
                            'mode/htmlmixed/htmlmixed.js',
                            'mode/javascript/javascript.js',
                            'mode/xml/xml.js'
                        ]
                    }
                ],

                'handsontable': [
                    'dist/jquery.handsontable.full.css',
                    'dist/jquery.handsontable.full.js'
                ],

                'husl': [
                    'husl.js'
                ],

                'jquery': [
                    'jquery.js'
                ],

                'jsdiff': [
                    'diff.js'
                ],

                'leaflet': [
                    {
                        cwd: 'dist',
                        dest: 'leaflet',
                        expand: true,
                        src: [
                            '*.css',
                            'images/**'
                        ]
                    },

                    {
                        dest: 'leaflet.js',
                        src: 'dist/leaflet-src.js'
                    }
                ],

                'leaflet.draw': [
                    {
                        cwd: 'dist',
                        dest: 'leaflet',
                        expand: true,
                        src: [
                            '*.css',
                            'images/**'
                        ]
                    },

                    {
                        dest: 'leaflet.draw.js',
                        src: 'dist/leaflet.draw-src.js'
                    }
                ],

                'L.GeoSearch': [
                    {
                        cwd: 'src/css',
                        dest: 'leaflet',
                        expand: true,
                        src: '*.css'
                    },

                    {
                        cwd: 'src/js',
                        expand: true,
                        src: '**/*.js'
                    }
                ],

                'leaflet.locatecontrol': [
                    {
                        cwd: 'dist',
                        dest: 'leaflet',
                        expand: true,
                        src: '*.css'
                    },

                    {
                        cwd: 'src',
                        expand: true,
                        src: '*.js'
                    }
                ]

            },

            styles: {
                dir: 'style',
                less: [ 'v3.less' ]
            },

            scripts: {
                dir: 'script',
                rjsModules: [
                    {
                        name: 'v3'
                    }
                ]
            }
        },

        less: {
            compile: {
                options: {
                    relativeUrls: true
                }
            }
        },
    });
};
