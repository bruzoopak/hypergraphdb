/*
 * This file is part of the HyperGraphDB source distribution. This is copyrighted
 * software. For permitted uses, licensing options and redistribution, please see
 * the LicensingInformation file at the root level of the distribution.
 *
 * Copyright (c) 2005-2023 Kobrix Software, Inc.  All rights reserved.
 *
 */

package org.hypergraphdb.storage.rocksdb;

import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.storage.HGIndexStats;

public class RocksDBIndexStats<IndexKey, IndexValue> implements HGIndexStats<IndexKey, IndexValue>
{
    private final RocksDBIndex<IndexKey, IndexValue> rocksDBIndex;

    public RocksDBIndexStats(RocksDBIndex<IndexKey, IndexValue> rocksDBIndex)
    {
        this.rocksDBIndex = rocksDBIndex;
    }

    //            private HGIndexStats(HGIndex index)
    //            {
    //
    //            }
    @Override
    public Count entries(long cost, boolean isEstimateOk)
    {
        if (cost < Long.MAX_VALUE)
        {
            if (!isEstimateOk)
            {
            /*
            Estimate is not ok, but that
            means we need a full scan which is above the
            requested maximum cost.
             */
                return null;
            }
            else
            {
                return new Count(() -> {
                    //all the values in the entire index
                    return rocksDBIndex.estimateIndexSize();
                }, true);
            }
        }
        else
        {
            return new Count(() -> {
                //all the values in the entire index
                try (var keys = (IteratorResultSet<HGPersistentHandle>) rocksDBIndex.scanKeys())
                {
                    return (long) keys.count();
                }
            }, false);
        }
    }

    @Override
    public Count keys(long cost, boolean isEstimateOk)
    {
        if (cost < Long.MAX_VALUE)
        {
            /*
            we cannot do anything for less than full scan
             */
            return null;
        }
        else
        {
            /*
            TODO count unique keys
                rocksDBIndex.scanKeys() returns all keys
             */
            return new Count(() -> {
                //all the values in the entire index
                try (var keys = (IteratorResultSet<HGPersistentHandle>) rocksDBIndex.scanKeys())
                {
                    return (long) keys.count();
                }
            }, false);
        }
    }

    @Override
    public Count valuesOfKey(IndexKey key, long cost, boolean isEstimateOk)
    {
        if (cost < Long.MAX_VALUE)
        {
            if (!isEstimateOk)
            {
                /*
                Estimate is not ok, but that
                means we need a full scan which is above the
                requested maximum cost.
                 */
                return null;
            }
            else
            {
                return new Count(() -> {
                    return rocksDBIndex.estimateIndexRange(
                            VarKeyVarValueColumnFamilyMultivaluedDB.firstRocksDBKey(
                                    rocksDBIndex.keyConverter.toByteArray(key)),
                            VarKeyVarValueColumnFamilyMultivaluedDB.lastRocksDBKey(
                                    rocksDBIndex.keyConverter.toByteArray(
                                            key)));
                }, true);
            }
        }
        else
        {
            return new Count(() -> {
                //all the values in the entire index
                try (var values = (IteratorResultSet<HGPersistentHandle>) rocksDBIndex.find(
                        key))
                {
                    return (long) values.count();
                }
            }, false);
        }
    }

    @Override
    public Count values(long cost, boolean isEstimateOk)
    {
        if (cost < Long.MAX_VALUE)
            return null;

        /*
        we do not have estimates for the
         */
        return new Count(() -> {
            //all the values in the entire index
            try (var values = (IteratorResultSet<HGPersistentHandle>) rocksDBIndex.scanValues())
            {
                return (long) values.count();
            }
        }, false);

    }

    @Override
    public Count keysWithValue(IndexValue o, long cost, boolean isEstimateOk)
    {
        /*
        TODO we can perform estimations
         */
        if (cost < Long.MAX_VALUE)
            return null;

        if (rocksDBIndex instanceof BidirectionalRocksDBIndex)
        {

            return new Count(() -> {
                //TODO get the estimate of the ra
                try (var keys = ((BidirectionalRocksDBIndex<IndexKey, IndexValue>)rocksDBIndex).findByValue(o))
                {
                    return (long) ((IteratorResultSet<IndexValue>)keys).count();
                }

            }, false);

        }
        else
        {
            return null;
        }

    }
}
