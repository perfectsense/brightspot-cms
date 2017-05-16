const gulp = require('gulp');
const args = require('minimist')(process.argv.slice(2));
const Styleguide = require('brightspot-styleguide/styleguide');
const styleguide = new Styleguide(args);

const systemjsConfig = {
    map: {
        'bsp-carousel': 'bower_components/bsp-carousel/dist/bsp-carousel/bsp-carousel.js',
        'bsp-utils': 'bower_components/bsp-utils/bsp-utils.js',
        'bsp-modal': 'bower_components/bsp-modal/src/js/bsp-modal.js',
        'masonry': 'bower_components/masonry/dist/masonry.pkgd.js',
        'jquery': 'bower_components/jquery/dist/jquery.js',
        'slick': 'bower_components/bsp-carousel/dist/bsp-carousel/slick.js',
        'vex': 'bower_components/vex/js/vex.js'
    }
}

gulp.task('css', () => {
    // TODO: lint less files via styleguide helper - maybe use? https://www.npmjs.com/package/lesshint

    const autoprefixer = require('autoprefixer');
    const less = require('gulp-less');
    const postcss = require('gulp-postcss');
    const sourcemaps = require('gulp-sourcemaps');

    return gulp.src('All.less')
        .pipe(sourcemaps.init())
        .pipe(less())
        .pipe(postcss([ autoprefixer('Last 2 versions') ]))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(styleguide.distPath()));
});

gulp.task('js', (done) => {
    // TODO: lint JS files via styleguide helper

    const Builder = require('systemjs-builder');
    const sourcemaps = require('gulp-sourcemaps');
    const uglify = require('gulp-uglify');
    const rename = require('gulp-rename');

    let builder = new Builder();

    builder.config(systemjsConfig);

    let buildOptions = {
        minify: false
    };

    builder.buildStatic('All.js', buildOptions).then((output) => {
        const file = require('gulp-file');

        gulp.src([ ])
            .pipe(file('All.js', output.source))
            .pipe(gulp.dest(styleguide.distPath()))
            .pipe(sourcemaps.init())
            .pipe(uglify())
            .pipe(rename({extname: '.min.js'}))
            .pipe(sourcemaps.write('.'))
            .pipe(gulp.dest(styleguide.distPath()))
            .on('end', done);
    });
});

gulp.task('watch', () => {
    // TODO: turn this into styleguide helper?
    gulp.watch([ '**/*.less', `!${styleguide.distPath()}/**`, '!bower_components/**', '!node_modules/**' ], [ 'css' ]);
    gulp.watch([ '**/*.js', `!${styleguide.distPath()}/**`, '!bower_components/**', '!node_modules/**' ], [ 'js' ]);
})

gulp.task('styleguide', () => {
    styleguide.serve({
        host: args.host,
        port: args.port
    });
});
