(ns watchlist.test
  (:require [doo.runner :refer-macros [doo-tests]]
            [watchlist.core-test]
            [watchlist.perf-test]))

(doo-tests 'watchlist.core-test
           'watchlist.perf-test)
