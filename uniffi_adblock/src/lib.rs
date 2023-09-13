/*
 * Copyright © 2022 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

use adblock::{request::Request, Engine};
use std::sync::Arc;

uniffi::include_scaffolding!("filter_engine");

// This is using the uniffi proc macros to simplify the uniffi integration instead of
// re-declaring the interface/functions in the udl file. See
// https://mozilla.github.io/uniffi-rs/proc_macro/index.html

#[derive(uniffi::Object)]
pub struct FilterEngine {
    engine: Engine,
}

#[uniffi::export]
impl FilterEngine {
    #[uniffi::constructor]
    pub fn new(rules: Vec<String>) -> Arc<Self> {
      // Uniffi proc macros and udl syntax struggle with `Vec<&str>` so we use String instead.
        let mut filter_set = adblock::lists::FilterSet::new(true);
        let parse_options = adblock::lists::ParseOptions {
            ..adblock::lists::ParseOptions::default()
        };
        for rule in rules {
            filter_set.add_filter_list(&rule, parse_options);
        }
        Arc::new(FilterEngine {
            engine: Engine::from_filter_set(filter_set, false),
        })
    }

    pub fn check_network_urls_matched(
        &self,
        url: String,
        source_url: String,
        request_type: String,
    ) -> bool {
        let request = Request::new(&url, &source_url, &request_type).unwrap();
        let result = self.engine.check_network_request(&request);
        return result.matched;
    }
}

/// We want some testing :)
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    pub fn internal() {
        println!("\n\n----- Start of Tests -----");

        let engine = FilterEngine::new(vec!["/button_facebook.".to_string()]);

        assert_eq!(
            engine.check_network_urls_matched(
                "https://example.com/button_facebook.".to_string(),
                "".to_string(),
                "script".to_string(),
            ),
            true
        );

        assert_eq!(
            engine.check_network_urls_matched(
                "https://example.com/foo.html".to_string(),
                "".to_string(),
                "script".to_string()
            ),
            false
        );
        
        println!("Passed");
    }
}
