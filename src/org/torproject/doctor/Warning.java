/* Copyright 2011 The Tor Project
 * See LICENSE for licensing information */
package org.torproject.doctor;

/* Warning about irregularities in parsed consensuses and votes. */
public enum Warning {

  /* No consensus is known that can be checked. */
  NoConsensusKnown,

  /* One or more directory authorities did not return a consensus within a
   * timeout of 60 seconds. */
  ConsensusDownloadTimeout,

  /* One or more directory authorities published a consensus that is more
   * than 1 hour old and therefore not fresh anymore. */
  ConsensusNotFresh,

  /* One or more directory authorities does not support the consensus
   * method that the consensus uses. */
  ConsensusMethodNotSupported,

  /* One or more directory authorities recommends different client
   * versions than the ones in the consensus. */
  DifferentRecommendedClientVersions,

  /* One or more directory authorities recommends different server
   * versions than the ones in the consensus. */
  DifferentRecommendedServerVersions,

  /* One or more directory authorities set unknown consensus
   * parameters. */
  UnknownConsensusParams,

  /* One or more directory authorities set conflicting consensus
   * parameters. */
  ConflictingConsensusParams,

  /* The certificate(s) of one or more directory authorities expire within
   * the next three months, which we warn about just once. */
  CertificateExpiresInThreeMonths,

  /* The certificate(s) of one or more directory authorities expire within
   * the next two months, which we warn about once per week. */
  CertificateExpiresInTwoMonths,

  /* The certificate(s) of one or more directory authorities expire within
   * the next 14 days, which we warn about once per day. */
  CertificateExpiresInTwoWeeks,

  /* The vote(s) of one or more directory authorities are missing. */
  VotesMissing,

  /* One or more directory authorities are not reporting bandwidth scanner
   * results. */
  BandwidthScannerResultsMissing,

  /* The fresh consensuses downloaded from one or more authorities are
   * missing votes that are contained in fresh consensuses downloaded from
   * other authorities. */
  ConsensusMissingVotes,

  /* The fresh consensuses downloaded from one or more authorities are
   * missing signatures from previously voting authorities. */
  ConsensusMissingSignatures,

  /* One or more authorities are missing in the consensus. */
  MissingAuthorities,

  /* One or more relays running on the IP addresses and dir ports of the
   * authorities are using a different relay identity key than
   * expected. */
  UnexpectedFingerprints,

  /* One or more authorities are running an unrecommended Tor version. */
  UnrecommendedVersions
}
