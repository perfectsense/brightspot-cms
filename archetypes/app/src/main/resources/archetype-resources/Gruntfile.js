'use strict';

module.exports = function (grunt) {
    require('bsp-grunt')(grunt, {
        bsp: {
            styles: {
                dir: 'assets/styles',
                less: [ '*.less' ],
                autoprefixer: true
            },

            scripts: {
                dir: 'assets/scripts'
            }
        }
    });
};
