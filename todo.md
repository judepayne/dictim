## TODO


## New Features added

- [ ] wiki documentation
- [x] globs
- [x] classes
- [x] positions
- [x] vars
- [x] case insensitivty for keys (no change)
- [x] overrides (no change)
- [?] imports
- [ ] layers, scenarios, steps
- [ ] array notation (see classes, globs, SQL tables]
- [x] conn-refs
- [x] & character (globs)
- [ ] lists in globs. also see vars for example.
- [x] . character in quoted keys
- [x] parse line returns at end of d2 string
- [x] overrides test. null for connection refernces
- [x] null keyword

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






















