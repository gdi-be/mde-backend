## [2.0.1](https://github.com/gdi-be/mde-backend/compare/v2.0.0...v2.0.1) (2025-04-16)


### Bug Fixes

* fix exception handling when getting image size ([f4972ae](https://github.com/gdi-be/mde-backend/commit/f4972ae27dc3b09f7ea2e04108b9ec31463c8e3a))

# [2.0.0](https://github.com/gdi-be/mde-backend/compare/v1.4.1...v2.0.0) (2025-04-16)


### Bug Fixes

* remove outdated and unused base controller methods ([b125d46](https://github.com/gdi-be/mde-backend/commit/b125d46698db38f3a9e8e2afa4e33c448d853502))
* removes unused properties from ColumnInfo ([e20c472](https://github.com/gdi-be/mde-backend/commit/e20c47223d4b8ff700490b0a83e9408355c2ee58))
* replace delimiter ([794c88d](https://github.com/gdi-be/mde-backend/commit/794c88d6e6867f42b8ecade94c4242c9af789157))
* update role name handling ([76bf5a9](https://github.com/gdi-be/mde-backend/commit/76bf5a9f00a439b3884889e5f4b6ca27263808db))


### Features

* add server sent event handling ([c48312b](https://github.com/gdi-be/mde-backend/commit/c48312be361a439b2f282b0dea4c9748efd1b325))
* add user details endpoint ([6808464](https://github.com/gdi-be/mde-backend/commit/68084643c13152ec9f0afa3639a2f18e17fb2773))
* automatically update legend with/height for resolvable urls ([18c1c3e](https://github.com/gdi-be/mde-backend/commit/18c1c3eeb3fec4d53a6f26bc38cfe27c5fdf487c))
* introduces FeatureType model ([270f79f](https://github.com/gdi-be/mde-backend/commit/270f79f57f761618e13f62c8a65208ac905cc61d))
* map owner and team members when importing ([2c11378](https://github.com/gdi-be/mde-backend/commit/2c113787382225bf9c7eebcbf8435a969677328f))
* opt out user assignment on import ([6d711fb](https://github.com/gdi-be/mde-backend/commit/6d711fb8bb68b5944ab56f1da6d8e8d28f04173a))
* rename roles ([5c8ad5e](https://github.com/gdi-be/mde-backend/commit/5c8ad5e5b0ced65e862cf6514cff793d4e264352))
* require authentication for all requests (except swagger) ([d9f261d](https://github.com/gdi-be/mde-backend/commit/d9f261d77703a34de2928413b7de3f987533efb4))
* update layers ([8a13a62](https://github.com/gdi-be/mde-backend/commit/8a13a620b798f2947ac46817bd16f8e1fa76901b))


### BREAKING CHANGES

* removes properties from Layer and updates mapping of layers in JsonClientMetadata
* This requires authentication for /metadata/** GET requests
* removes several properties from ColumnInfo

## [1.4.1](https://github.com/gdi-be/mde-backend/compare/v1.4.0...v1.4.1) (2025-04-10)


### Bug Fixes

* update role assignment ([f1ecf94](https://github.com/gdi-be/mde-backend/commit/f1ecf941dfb2aa561bed487a0076b12e455f109d))

# [1.4.0](https://github.com/gdi-be/mde-backend/compare/v1.3.0...v1.4.0) (2025-04-10)


### Bug Fixes

* add missing properties ([68b4a8f](https://github.com/gdi-be/mde-backend/commit/68b4a8f13a8c53b7327d1d304ad3e8c6c06708ac))
* fix importer ([a4d0f3d](https://github.com/gdi-be/mde-backend/commit/a4d0f3d518926506d18b63a1085f67debb609aa9))
* update query config ([e860310](https://github.com/gdi-be/mde-backend/commit/e86031092db34ed517b02eb02c5d9ef5223531a7))


### Features

* add approved property and interfaces ([b0f9b44](https://github.com/gdi-be/mde-backend/commit/b0f9b448228fc5947eed1dec627c08fd55a954e0))

# [1.3.0](https://github.com/gdi-be/mde-backend/compare/v1.2.0...v1.3.0) (2025-04-09)


### Bug Fixes

* fix import/generation of certain fields ([99679e2](https://github.com/gdi-be/mde-backend/commit/99679e29a24de47e34a7dfe8da0208cf835f850e))


### Features

* enable openapi docs ([a2cd159](https://github.com/gdi-be/mde-backend/commit/a2cd1595fa94b13d0e5be226607d230d30cf91d7))

# [1.2.0](https://github.com/gdi-be/mde-backend/compare/v1.1.3...v1.2.0) (2025-04-07)


### Bug Fixes

* update automatic keyword generation ([035633e](https://github.com/gdi-be/mde-backend/commit/035633ecb7e621f38f0554c1a00ca6a3f451a5ea))


### Features

* add DELETE /metadata/{metadataId} interface ([d257af2](https://github.com/gdi-be/mde-backend/commit/d257af28f6f58282b852ae1a18c02e3aebd507ea))
* add INSPIRE validator test classes ([61c7d09](https://github.com/gdi-be/mde-backend/commit/61c7d09d3db4e37d7119312b882a3badfad6cf57))
* add keycloak client and role handling ([691b57b](https://github.com/gdi-be/mde-backend/commit/691b57ba893ba77600a74f462f1738008ee10688))
* add ownerId To MetadataCollection ([1e21648](https://github.com/gdi-be/mde-backend/commit/1e216484769e7f21266f084b2326160f42a2dee2))
* assign creator to metadata and add to team ([38f457d](https://github.com/gdi-be/mde-backend/commit/38f457d3e91bdca12ad55c4dce0d3af9de10d781))
* introduces AssignRoleData dto ([5ee5ff5](https://github.com/gdi-be/mde-backend/commit/5ee5ff5bbdc50f67d33ed255d5842d87c0f9a786))

## [1.1.3](https://github.com/gdi-be/mde-backend/compare/v1.1.2...v1.1.3) (2025-03-27)


### Bug Fixes

* update pipelines ([2eb6015](https://github.com/gdi-be/mde-backend/commit/2eb6015dd69a9db26e4c8cfba76fd07bc421eeb5))

## [1.1.2](https://github.com/gdi-be/mde-backend/compare/v1.1.1...v1.1.2) (2025-03-27)


### Bug Fixes

* add missing env variables ([a0506f9](https://github.com/gdi-be/mde-backend/commit/a0506f932d3531a329bacbd3790cd36c8248edab))

## [1.1.1](https://github.com/gdi-be/mde-backend/compare/v1.1.0...v1.1.1) (2025-03-27)


### Bug Fixes

* update on-release.yaml ([d306812](https://github.com/gdi-be/mde-backend/commit/d306812b7058d1b39e90505700df42adb594d681))

# [1.1.0](https://github.com/gdi-be/mde-backend/compare/v1.0.7...v1.1.0) (2025-03-27)


### Bug Fixes

* fix release workflow ([faf66ac](https://github.com/gdi-be/mde-backend/commit/faf66ac8971ca284b323acdf2b6236c685b68487))


### Features

* add downloads field to Service ([9383525](https://github.com/gdi-be/mde-backend/commit/9383525ad6e488c7fcff46747269e122ce330e0d))

## [1.0.7](https://github.com/gdi-be/mde-backend/compare/v1.0.6...v1.0.7) (2025-03-27)


### Bug Fixes

* fix release workflows ([e9e8710](https://github.com/gdi-be/mde-backend/commit/e9e87106938b6c6dc4b2dcdf41662e40ba16950d))
* fix syntax ([bc2d06a](https://github.com/gdi-be/mde-backend/commit/bc2d06ab1c71b90d61d7b721e1b568e007b49839))

## [1.0.6](https://github.com/gdi-be/mde-backend/compare/v1.0.5...v1.0.6) (2025-03-27)


### Bug Fixes

* fix release workflow ([8543b2f](https://github.com/gdi-be/mde-backend/commit/8543b2fc1816e288fabc82fa212a4f3bd7f774a4))

## [1.0.5](https://github.com/gdi-be/mde-backend/compare/v1.0.4...v1.0.5) (2025-03-27)


### Bug Fixes

* fix release workflow ([b6f8e73](https://github.com/gdi-be/mde-backend/commit/b6f8e7377a3272f1d8a57927e34094222d79ba73))

## [1.0.4](https://github.com/gdi-be/mde-backend/compare/v1.0.3...v1.0.4) (2025-03-27)


### Bug Fixes

* fix release workflow ([bbc36cf](https://github.com/gdi-be/mde-backend/commit/bbc36cfec904d8ff461ef94daf9248b4471c75d6))

## [1.0.3](https://github.com/gdi-be/mde-backend/compare/v1.0.2...v1.0.3) (2025-03-27)


### Bug Fixes

* fix release workflow ([8d11fff](https://github.com/gdi-be/mde-backend/commit/8d11fff72b2141c46307e93db44eff87e4c7cca8))

## [1.0.2](https://github.com/gdi-be/mde-backend/compare/v1.0.1...v1.0.2) (2025-03-27)


### Bug Fixes

* fix release workflow ([1fd07e5](https://github.com/gdi-be/mde-backend/commit/1fd07e5fa378e0e0c62f131be08787f468579c8f))

## [1.0.1](https://github.com/gdi-be/mde-backend/compare/v1.0.0...v1.0.1) (2025-03-27)


### Bug Fixes

* fix output version for build pipeline ([829b376](https://github.com/gdi-be/mde-backend/commit/829b3765b70551c2c6dac63fb2e5fe4ef2baa882))

# 1.0.0 (2025-03-27)


### Bug Fixes

* add login to registry ([d73b279](https://github.com/gdi-be/mde-backend/commit/d73b27975879ffc94622b7d525c4a22bb8e601c2))
* add parsing of extent and INSPIRE theme ([e1f7450](https://github.com/gdi-be/mde-backend/commit/e1f745049dd7bb35de619c47a25812d763b9bf49))
* add releaserc ([d16d2da](https://github.com/gdi-be/mde-backend/commit/d16d2da13b149767aa9b9e6b57b5c76cd9bda10a))
* add trace logging ([5556a1e](https://github.com/gdi-be/mde-backend/commit/5556a1e3da7bc23cf9b1fdad78d0ed2d9f69b057))
* clean up previews and phone numbers ([0f6110c](https://github.com/gdi-be/mde-backend/commit/0f6110c83e152b9757c2eea26874797e5fb84954))
* fix building of image ([255cf50](https://github.com/gdi-be/mde-backend/commit/255cf50ae8be0c4d8349abaf092e3dafc96940dd))
* fix building of image ([c603f58](https://github.com/gdi-be/mde-backend/commit/c603f5809366659b05baca1d8eb2a96485309f10))
* fix compile error ([4df9fb4](https://github.com/gdi-be/mde-backend/commit/4df9fb461da3924a4648a6a2dad452e593fb3fe5))
* fix INSPIRE validation errors ([28d8564](https://github.com/gdi-be/mde-backend/commit/28d8564a63cc9a5f741ca88df005b2b256c614b3))
* fix phone sanitizing ([cb706f8](https://github.com/gdi-be/mde-backend/commit/cb706f8eb2724158ef3abfb8ea736ffd890b8973))
* fix several import/export issues ([c2b0088](https://github.com/gdi-be/mde-backend/commit/c2b00887fbecbbfa8f281b0b3c6a684b039767f7))
* generate valid ISO metadata ([50ba878](https://github.com/gdi-be/mde-backend/commit/50ba8788f73f3583d006afcc10f9266a2e8a3524))
* properly configure logging ([ce5cdc7](https://github.com/gdi-be/mde-backend/commit/ce5cdc767472c2fd571e16bf57da7d610d01bd2d))
* refactor JSON data to be single instance instead of List ([499ca3c](https://github.com/gdi-be/mde-backend/commit/499ca3ca29464b3f2eb832abbe59868b5cf0b85e))
* remove unnecessary push ([67c3dfc](https://github.com/gdi-be/mde-backend/commit/67c3dfc6837bb8a9d8579e9e6a52fa9e52ea1c6f))
* remove unneeded properties ([4f33fe7](https://github.com/gdi-be/mde-backend/commit/4f33fe77cfbf7ef49e60720dd0de6bc97cc63845))
* rename migration file ([56e1f7e](https://github.com/gdi-be/mde-backend/commit/56e1f7e454af38a3b6034dbd8407aed89b4460e5))
* restore old search interface ([4c61af2](https://github.com/gdi-be/mde-backend/commit/4c61af28ec70b93328d12e2b248f3a8aab9f426f))
* set isoMetadata title on creation ([417f3ae](https://github.com/gdi-be/mde-backend/commit/417f3ae8d90b4cc4d202902248e97d6474511d5a))
* update docs and messages ([10a0066](https://github.com/gdi-be/mde-backend/commit/10a0066202c55360f22cc3de8bebdaf6c6745de8))
* update message codes ([4c4ea79](https://github.com/gdi-be/mde-backend/commit/4c4ea794adbe5348bac9adb7d9e9207a66df1333))
* use openjdk base image ([b72cd24](https://github.com/gdi-be/mde-backend/commit/b72cd249a0f29c00c339c88034fa2000d0d6a829))
* use proper base image ([4273144](https://github.com/gdi-be/mde-backend/commit/4273144e83de34b7d7e36b367ead9eb1e0ba71b7))
* use proper base image ([4e6390d](https://github.com/gdi-be/mde-backend/commit/4e6390d17f1e0da2b806d7717a2b1d3fff29ef22))


### Features

* a serach index interfaces ([1806634](https://github.com/gdi-be/mde-backend/commit/180663481e238a6227f59777753a41a11216bae3))
* adapt import and export to use terms of use codelist ([7a984a7](https://github.com/gdi-be/mde-backend/commit/7a984a7fcef22140cc014d804acc40c56eba8728))
* add actuator endpoints ([9c84370](https://github.com/gdi-be/mde-backend/commit/9c84370c410146157d1e0a5efbe3682a00b2ab4c))
* add additional sort by modified ([ba61286](https://github.com/gdi-be/mde-backend/commit/ba612865e9abac552105a47eca62737925a6c640))
* add automatic keywords ([75a43a5](https://github.com/gdi-be/mde-backend/commit/75a43a57eb3d947acfea0df6bb02246291d7a460))
* add ci build ([3634909](https://github.com/gdi-be/mde-backend/commit/3634909d1a02e0fcb22088a66078fdc250c051c4))
* add data model ([311c1e2](https://github.com/gdi-be/mde-backend/commit/311c1e26dba8d6a0f28e9a107b2e269ca5a1fe6a))
* add deletion of comments ([9e71ab0](https://github.com/gdi-be/mde-backend/commit/9e71ab0a5c37c5f225575e904a84701a563678ee))
* add findByMetadataId interfaces ([46b9d53](https://github.com/gdi-be/mde-backend/commit/46b9d53f26e2e7834c042fab0ba2ee6fc415b973))
* add more inspire themes ([a5efbcd](https://github.com/gdi-be/mde-backend/commit/a5efbcdbd39f72dd3b7198fc1fb93569ec0a0419))
* add oauth handling ([06a2361](https://github.com/gdi-be/mde-backend/commit/06a2361b4194eeddc78849cdeef35eb1a436261c))
* add responsibility assignment interfaces ([387345e](https://github.com/gdi-be/mde-backend/commit/387345e9e76eb677496e0e1c810e020f028ae74f))
* add responsibility unassign interfaces ([c581bef](https://github.com/gdi-be/mde-backend/commit/c581befdc1725b44f38e055008b1cc47f94bd39d))
* add service and controller for isometadata ([02bbf88](https://github.com/gdi-be/mde-backend/commit/02bbf887f986fa74859f3fd08a6d976c5249832e))
* add support for cloning ([394d481](https://github.com/gdi-be/mde-backend/commit/394d481f4fdf09a94b6e06b292c6e854123a9a49))
* add termsOfUseId ([c7b6034](https://github.com/gdi-be/mde-backend/commit/c7b6034557ba3c7308c28706a3d5d14a8614cd33))
* add updateTitle interfaces ([2e6e48b](https://github.com/gdi-be/mde-backend/commit/2e6e48bd658a132d183d22dbfae29286003fffce))
* add variable replacement when generating metadata ([1e1227b](https://github.com/gdi-be/mde-backend/commit/1e1227bf79b308db5ea2cacb054489ed112d6783))
* amend README with link to importer ([173dc94](https://github.com/gdi-be/mde-backend/commit/173dc947844d87f35f26afe101ac5032ff3a4db5))
* complete service metadata generation ([2b23008](https://github.com/gdi-be/mde-backend/commit/2b2300822a9cfce56ee98d42309d9faee2c1bf51))
* extend search to use description ([ad5babf](https://github.com/gdi-be/mde-backend/commit/ad5babf43cb2d9f57c17315f5bbe7852c58ed174))
* finish publication ([564ad16](https://github.com/gdi-be/mde-backend/commit/564ad161e8f51e5bf61a880ac55c52b26128b107))
* implement importer ([838cec3](https://github.com/gdi-be/mde-backend/commit/838cec3870dadda592eb9863d91e9552e6a9cdd3))
* implement ISO generation (datasets) ([f29b906](https://github.com/gdi-be/mde-backend/commit/f29b906bbabce9b04efa83fdb589cd9c3213364a))
* import capabilities ([f3727a0](https://github.com/gdi-be/mde-backend/commit/f3727a00995b9771e78ae925376e4f91792fa84e))
* initial setup to write service metadata ([f774aba](https://github.com/gdi-be/mde-backend/commit/f774aba58024290d0e1b622b3ba8ea667a64b245))
* initialize backend ([8bd1c98](https://github.com/gdi-be/mde-backend/commit/8bd1c9816c442e57d00845253e7f9a01311a0dd9))
* introduce SearchResponse ([0a8e0c6](https://github.com/gdi-be/mde-backend/commit/0a8e0c6f523936e74ceec79b95408c57c5569543))
* introduce update interface ([592fe4d](https://github.com/gdi-be/mde-backend/commit/592fe4d0e8adffb68ce723637a285b4abb0106b0))
* introduces create interface ([fd2ae0c](https://github.com/gdi-be/mde-backend/commit/fd2ae0c7f3dbe1b4f2dfc9994c03349ac39429f0))
* introduces hiberante search ([2f6e2ad](https://github.com/gdi-be/mde-backend/commit/2f6e2adcd277079b07ac501f1a7186c6216a52e2))
* introduces MetadataCollection DTO ([7308ec9](https://github.com/gdi-be/mde-backend/commit/7308ec96ccbafb7ae6c04be24070baa3f5a537e9))
* introduces privacy ENUM ([5244b4d](https://github.com/gdi-be/mde-backend/commit/5244b4d112231b1db6ba993b597534df4dc97584))
* prepare Comments ([470c84f](https://github.com/gdi-be/mde-backend/commit/470c84f35c3307c86ba541d0a26c41c14ace9f22))
* prepare metadata controller ([1a82bd0](https://github.com/gdi-be/mde-backend/commit/1a82bd0cfc8f9931ed016210c431f308ff0d6d80))
* prepare PermissionEvaluator ([09c919e](https://github.com/gdi-be/mde-backend/commit/09c919e4c52591284d20b98f6c09c418bf267c02))
* publish maven packages to access importer ([33c2762](https://github.com/gdi-be/mde-backend/commit/33c27628084d63ef6e8be5092678bc1b0221af47))
* refactor base models ([d8bff72](https://github.com/gdi-be/mde-backend/commit/d8bff72d7f0095321eb8e4c5d31be1b1e3df1762))
* reindex search items on startup ([c813348](https://github.com/gdi-be/mde-backend/commit/c813348dc1b95771cf02c03a238c04d3672b05ca))
* remove metadataProfile from creation ([4c36903](https://github.com/gdi-be/mde-backend/commit/4c369036ebace00fb03a1dc593e3a33db991bf87))
* remove title from BaseMetadata ([2f96e6a](https://github.com/gdi-be/mde-backend/commit/2f96e6ae747e21598bae36f0585141da187342b6))
* replace lucene search with hibernate query and add sorting ([fbaa832](https://github.com/gdi-be/mde-backend/commit/fbaa8321224f4e11fcac5112496531196ff7aaea))
* update importer ([e822544](https://github.com/gdi-be/mde-backend/commit/e822544ae5f9a037a5c3031a2f5709a87646ac30))
* update importer ([7b225fd](https://github.com/gdi-be/mde-backend/commit/7b225fde22f10e0472e22db7e83da33110b0b116))
* update importer ([2da8ebc](https://github.com/gdi-be/mde-backend/commit/2da8ebc2df75d4e2fb758d428c0593d79c03610f))
* update JsonIsoMetadata ([315ec19](https://github.com/gdi-be/mde-backend/commit/315ec19afb7f1c62aa240d4a97b5fd0b989c6070))
* update search interface for custom filter ([f291001](https://github.com/gdi-be/mde-backend/commit/f291001e59cac2bdae7fc9eb5c5dfb5bd887958a))
* use semantic release & tagged releases ([6bfbf04](https://github.com/gdi-be/mde-backend/commit/6bfbf04d335d4ace68ebc34592ebd8a25739e95a))
* validate and publish metadata ([c060f95](https://github.com/gdi-be/mde-backend/commit/c060f954e42dd54d0d23e8254a8e150990537978))


### BREAKING CHANGES

* this removes the ClientMetadata, IsoMetadata and TechnicalMetadata. Their corresponding json values are now stored in the new MetadataCollection entity
