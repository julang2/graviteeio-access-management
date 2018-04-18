/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.am.repository.mongodb.oauth2;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.reactivestreams.client.MongoCollection;
import io.gravitee.am.repository.mongodb.oauth2.internal.model.AccessTokenMongo;
import io.gravitee.am.repository.oauth2.api.AccessTokenRepository;
import io.gravitee.am.repository.oauth2.model.AccessToken;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.bson.Document;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoAccessTokenRepository extends AbstractOAuth2MongoRepository implements AccessTokenRepository {

    private MongoCollection<AccessTokenMongo> accessTokenCollection;

    private static final String FIELD_RESET_TIME = "expire_at";
    private static final String FIELD_CLIENT_ID = "client_id";
    private static final String FIELD_TOKEN = "token";
    private static final String FIELD_ID = "_id";

    @PostConstruct
    public void init() {
        accessTokenCollection = mongoOperations.getCollection("access_tokens", AccessTokenMongo.class);
        accessTokenCollection.createIndex(new Document(FIELD_CLIENT_ID, 1)).subscribe(new LoggableIndexSubscriber());
        accessTokenCollection.createIndex(new Document(FIELD_TOKEN, 1)).subscribe(new LoggableIndexSubscriber());
        accessTokenCollection.createIndex(new Document(FIELD_RESET_TIME, 1), new IndexOptions().expireAfter(0L, TimeUnit.SECONDS)).subscribe(new LoggableIndexSubscriber());
    }

    private Maybe<AccessToken> findById(String id) {
        return Observable
                .fromPublisher(accessTokenCollection.find(eq(FIELD_ID, id)).limit(1).first())
                .firstElement()
                .map(this::convert);
    }

    @Override
    public Maybe<AccessToken> findByToken(String token) {
        return Observable
                .fromPublisher(accessTokenCollection.find(eq(FIELD_TOKEN, token)).limit(1).first())
                .firstElement()
                .map(this::convert);
    }

    @Override
    public Single<AccessToken> create(AccessToken accessToken) {
        return Single
                .fromPublisher(accessTokenCollection.insertOne(convert(accessToken)))
                .flatMap(success -> findById(accessToken.getId()).toSingle());
    }

    @Override
    public Completable delete(String token) {
        return Completable.fromPublisher(accessTokenCollection.findOneAndDelete(eq(FIELD_TOKEN, token)));
    }

    private AccessTokenMongo convert(AccessToken accessToken) {
        if (accessToken == null) {
            return null;
        }

        AccessTokenMongo accessTokenMongo = new AccessTokenMongo();
        accessTokenMongo.setId(accessToken.getId());
        accessTokenMongo.setToken(accessToken.getToken());
        accessTokenMongo.setClientId(accessToken.getClientId());
        accessTokenMongo.setCreatedAt(accessToken.getCreatedAt());
        accessTokenMongo.setExpireAt(accessToken.getExpireAt());
        accessTokenMongo.setRefreshToken(accessToken.getRefreshToken());

        return accessTokenMongo;
    }

    private AccessToken convert(AccessTokenMongo accessTokenMongo) {
        if (accessTokenMongo == null) {
            return null;
        }

        AccessToken accessToken = new AccessToken();
        accessToken.setId(accessTokenMongo.getId());
        accessToken.setToken(accessTokenMongo.getToken());
        accessToken.setClientId(accessTokenMongo.getClientId());
        accessToken.setCreatedAt(accessTokenMongo.getCreatedAt());
        accessToken.setExpireAt(accessTokenMongo.getExpireAt());
        accessToken.setRefreshToken(accessTokenMongo.getRefreshToken());

        return accessToken;
    }
}