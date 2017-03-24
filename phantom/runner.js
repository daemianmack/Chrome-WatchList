var page = require('webpage').create();
var system = require('system');

if (system.args.length < 2) {
  console.log('Expected a target URL parameter.');
  phantom.exit(1);
}

var url = system.args[1];

var perf = system.env.PERF;


page.onConsoleMessage = function (message) {
  console.log(message);
};


/*
There are a number of outstanding issues with running phantomjs in
this manner that I haven't been able to resolve. Luckily they mostly
matter only when executing one-off test runs, and matter less when in
continuous autotesting mode.

1 - Phantom will not recognize any attempt to exit with a specific
exit code, so while we can print test failures, we cannot let the
shell become aware of them.

2 - Phantom takes an extra ~60 seconds to exit after a call to `phantom.exit()`.
*/
if (perf) {
    page.open(url, function (status) {
        if (status !== "success") {
            console.log('Failed to open ' + url);
            setTimeout(function() { // https://github.com/ariya/phantomjs/issues/12697
                phantom.exit(1);
            }, 0);
        }

        page.evaluate(function() {
            watchlist.test.perf_bench();
        });

        setTimeout(function() { // https://github.com/ariya/phantomjs/issues/12697
            phantom.exit(0);
        }, 0);
    });
}

else {
    page.open(url, function (status) {
        if (status !== "success") {
            console.log('Failed to open ' + url);
            setTimeout(function() { // https://github.com/ariya/phantomjs/issues/12697
                phantom.exit(1);
            }, 0);
        }

         page.evaluate(function() {
            watchlist.test.run_tests();
        });

        setTimeout(function() { // https://github.com/ariya/phantomjs/issues/12697
            phantom.exit(0);
        }, 0);
    });

}
