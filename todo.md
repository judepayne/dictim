## TODO

- [x] Add fill pattern test
- [x] Add fill pattern
- [ ] Write validation functions for d2 attributes
- [x] Write compile tests for globs
- [x] write parse tests for globs
- [ ] Add note to syntax wiki about $ character; needs to be single quoted
- [x] add class as a d2 keyword: validate-fn map?
- [x] test handling of .class endings to d2 keys
- [ ] (a -> b)[0].class: something -> edge refs
- [x] write classes tests for both compilation and parsing.
- [x] positions




## New Features added

- [x] globs
- [x] classes
- [x] positions
- [x] vars
- [x] case insensitivty for keys (no change)
- [x] overrides (no change)
- [?] imports
- [ ] layers, scenarios, steps
- [ ] array notation (see classes, globs, SQL tables]
- [-] conn-refs
- [ ] & character (globs)
- [ ] lists in globs
- [x] . character in quoted keys

### design thoughts

- & character (see globs)
- . character must be quote if in a key.
The quote must be around the whole string.


no special processing required. just write tests.

- vars
- overrides -> no need to handle
- imports
- layers, scenarios, steps

like classes. these are a special ctr. no extra processing needed asides from possibly for links.
double check that link: layers.cat would be parsed correctly.

- imports -> no special processing for dictim. if '@' handled, then already handled






















