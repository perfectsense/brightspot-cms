define(['jquery'], function($) {
    
    var spellcheck;

    spellcheck = {

        
        // Cache object to save spelling words that have been previously looked up
        cache: {},

        
        /**
         * The URL of the spell check service
         */
        serviceUrl: window.CONTEXT_PATH + '/spellCheck',

        
        /** The spelling locale will be read from the HTML element in the lang attribute,
         * but if not found there use this locale.
         */
        localeDefault: 'en',

        
        /**
         * Lookup one or more words
         *
         * @param {String|Array} words
         * A single word or multiple words to look up.
         *
         * @param {Object} [options]
         * @param {Object} [options.useCache=true]
         * @param {Object} [options.locale]
         *
         * @returns {Promise}
         *
         * A promise that can be used to retrieve results afer the async call is complete.
         *
         * If successful the promise resolves with an object of key,value pairs where
         * the key is the word that was looked up and the value is the result.
         * The result is null if the spelling is correct or there are no suggestions,
         * or it is an array of suggested corrections.
         *
         * If unseccessful the promise fails with a text status:
         * notsupported = the locale is not supported for spell check
         * ("notmodified", "nocontent", "error", "timeout", "abort", or "parsererror")
         *
         * @example
         * words = ['apple', 'orange'];
         * spellcheck.lookup().done(function(results) {
         *   $.each(words, function(){
         *       var result, word;
         *       word = this;
         *       result = results[word];
         *       if ($.isArray(result)) {
         *           // result is spelling suggestions array 
         *       }
         *   });
         * }).fail(function(status) {
         *    // Check status 
         * });
         */
        lookup: function(words, options) {
            
            var cachedResult, deferred, self, results, word, wordsArray, wordsToFetch, wordsUnique;
            
            self = this;

            options = $.extend({}, {
                useCache:true,
                locale:self.localeGet()
            }, options);

            // The deferred object used to return a promise
            deferred = $.Deferred();

            // Results for the words
            results = {};

            // Make sure we have an array of words (in case a single word was supplied)
            wordsArray = $.isArray(words) ? words : [words];

            // Eliminate any duplicate words
            wordsUnique = {};
            $.each(wordsArray, function(){
                wordsUnique[this] = true;
            });

            // First check if any of the words are already in the cache so we can avoid making a call to the service
            // Note we are using a for..in statement here because jquery.each() has a problem dealing with the word
            // "length", since it uses that to determine if is is dealing with an object or an array.
            wordsToFetch = [];
            for (word in wordsUnique) {
                
                // Make sure this is actually a word and not some other object property
                if(!wordsUnique.hasOwnProperty(word)) {
                    continue;
                }
                
                cachedResult = self.cacheLookup(word);
                if (cachedResult === undefined || !options.useCache) {
                    wordsToFetch.push(word);
                } else {
                    results[word] = cachedResult;
                }
            }

            // If there are words that are not already cached, make a single ajax call to fetch them all
            if (wordsToFetch.length) {

                $.ajax(self.serviceUrl, {

                    type: 'POST',
                    
                    dataType: 'json',
                    
                    // For array of words, use multiple parameters like word=apple&word=orange
                    traditional:true,
                    
                    data: {
                        locale: options.locale,
                        word: wordsToFetch
                    }
                    
                }).done(function(data, textStatus, jqXHR){

                    // Data returned will be for example:
                    // /cms/spellCheck?locale=en&word=perfcet&word=sense
                    // {"status":"supported","results":[["perfect"],null]}
                    
                    // Check if the locale provided is supported
                    if (data.status === 'supported') {

                        // Match up the returned value with the original word
                        $.each(wordsToFetch, function(i, word) {
                            var result;
                            result = data.results[i];
                            results[word] = result;
                            if (options.useCache) {
                                self.cacheAdd(word, result);
                            }
                        });

                        deferred.resolve(results);
                        
                    } else {
                        
                        // The locale is not supported
                        deferred.reject(data.status || 'unsupported');
                    }
                    
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    
                    // The ajax call failed for some reason
                    // textStatus will be ("notmodified", "nocontent", "error", "timeout", "abort", or "parsererror")
                    deferred.reject(textStatus);
                    
                });
                
            } else {
                // All the words were in the cache so resolve the deferred object immediately
                deferred.resolve(results);
            }

            // Return the deferred promise so caller can get the results
            return deferred.promise();
        },

        
        /**
         * Clear the cache.
         */
        cacheClear: function() {
            var self;
            self = this;
            self.cache = {};
        },


        /**
         * Add a result to the cache.
         */
        cacheAdd: function(word, result) {
            var self;
            self = this;
            self.cache[word] = result;
        },

        
        /**
         * Look up one or more words in the cache.
         * @returns {Array|null|undefined}
         */
        cacheLookup: function(word) {
            var self;
            self = this;
            return self.cache[word];
        },

        
        /**
         * Get the locale value, which will be set on the HTML element in the lang attribute.
         * If not set there use localeDefault instead.
         */
        localeGet: function() {
            
            var locale, self;

            self = this;
            
            locale = $('html').attr('lang') || self.localeDefault;
            
            return locale.toLowerCase();
        }
        
    };

    return spellcheck;
});
