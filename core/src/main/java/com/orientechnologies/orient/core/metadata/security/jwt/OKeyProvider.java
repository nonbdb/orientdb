package com.orientechnologies.orient.core.metadata.security.jwt;

import java.security.Key;

/**
 * Created by emrul on 28/09/2014.
 *
 * @author Emrul Islam <emrul@emrul.com> Copyright 2014 Emrul Islam
 */
public interface OKeyProvider {

  Key getKey(OTokenHeader header);

  String[] getKeys();

  String getDefaultKey();
}
