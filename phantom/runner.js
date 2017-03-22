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



if (perf) {
    page.open(url, function (status) {
        if (status !== "success") {
            console.log('Failed to open ' + url);
            setTimeout(function() { // https://github.com/ariya/phantomjs/issues/12697
                phantom.exit(1);
            }, 0);
        }

        page.evaluate(function() {
            watchlist.test.perf_bench()
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
            watchlist.test.run_tests()
        });

        setTimeout(function() { // https://github.com/ariya/phantomjs/issues/12697
            phantom.exit(0);
        }, 0);
    });

}
