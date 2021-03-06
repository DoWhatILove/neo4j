/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.edge;

import java.io.IOException;

import org.neo4j.coreedge.catchup.storecopy.CopiedStoreRecovery;
import org.neo4j.coreedge.catchup.storecopy.LocalDatabase;
import org.neo4j.coreedge.catchup.storecopy.StoreCopyFailedException;
import org.neo4j.coreedge.catchup.storecopy.StoreFetcher;
import org.neo4j.coreedge.catchup.storecopy.StreamingTransactionsFailedException;
import org.neo4j.coreedge.catchup.storecopy.TemporaryStoreDirectory;
import org.neo4j.coreedge.identity.MemberId;
import org.neo4j.coreedge.identity.StoreId;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.logging.Log;

public class CopyStoreSafely
{
    private final FileSystemAbstraction fs;
    private final LocalDatabase localDatabase;
    private final CopiedStoreRecovery copiedStoreRecovery;
    private final Log log;

    public CopyStoreSafely( FileSystemAbstraction fs, LocalDatabase localDatabase, CopiedStoreRecovery copiedStoreRecovery, Log log )
    {
        this.fs = fs;
        this.localDatabase = localDatabase;
        this.copiedStoreRecovery = copiedStoreRecovery;
        this.log = log;
    }

    public void copyWholeStoreFrom( MemberId source, StoreId expectedStoreId, StoreFetcher storeFetcher )
            throws IOException, StoreCopyFailedException, StreamingTransactionsFailedException
    {
        try ( TemporaryStoreDirectory tempStore = new TemporaryStoreDirectory( fs, localDatabase.storeDir() ) )
        {
            storeFetcher.copyStore( source, expectedStoreId, tempStore.storeDir() );
            copiedStoreRecovery.recoverCopiedStore( tempStore.storeDir() );
            localDatabase.replaceWith( tempStore.storeDir() );
        }
        log.info( "Replaced store with one downloaded from %s", source );
    }
}
