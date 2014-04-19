/* Copyright 2011 The Tor Project
 * See LICENSE for licensing information */
package org.torproject.doctor;

import java.io.*;
import java.text.*;
import java.util.*;
import org.torproject.descriptor.*;

/* Transform the most recent consensus and corresponding votes into an
 * HTML page showing possible irregularities. */
public class MetricsWebsiteReport {

  /* Date-time format to format timestamps. */
  private static SimpleDateFormat dateTimeFormat;
  static {
    dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /* Output file to write report to. */
  private File htmlOutputFile =
      new File("out/website/consensus-health.html");

  /* Store the downloaded consensus and corresponding votes for later
   * processing. */
  private RelayNetworkStatusConsensus downloadedConsensus;
  private SortedMap<String, RelayNetworkStatusVote> downloadedVotes =
      new TreeMap<String, RelayNetworkStatusVote>();
  public void processDownloadedConsensuses(
      List<DescriptorRequest> downloads) {
    long mostRecentValidAfterMillis = -1L;
    for (DescriptorRequest request : downloads) {
      if (request.getDescriptors() == null) {
        continue;
      }
      for (Descriptor descriptor : request.getDescriptors()) {
        if (descriptor instanceof RelayNetworkStatusConsensus) {
          RelayNetworkStatusConsensus downloadedConsensus =
              (RelayNetworkStatusConsensus) descriptor;
          if (downloadedConsensus.getValidAfterMillis() >
              mostRecentValidAfterMillis) {
            this.downloadedConsensus = downloadedConsensus;
            mostRecentValidAfterMillis =
                downloadedConsensus.getValidAfterMillis();
          }
        } else if (descriptor instanceof RelayNetworkStatusVote) {
          RelayNetworkStatusVote vote =
              (RelayNetworkStatusVote) descriptor;
          this.downloadedVotes.put(vote.getNickname(), vote);
        } else {
          System.err.println("Did not expect a descriptor of type "
              + descriptor.getClass() + ".  Ignoring.");
        }
      }
    }
  }

  /* Store the DownloadStatistics reference to request download statistics
   * when writing the report. */
  private DownloadStatistics statistics;
  public void includeFetchStatistics(
      DownloadStatistics statistics) {
    this.statistics = statistics;
  }

  /* Writer to write all HTML output to. */
  private BufferedWriter bw;

  /* Write HTML output file for the metrics website. */
  public void writeReport() {

    if (this.downloadedConsensus != null) {
      try {
        this.htmlOutputFile.getParentFile().mkdirs();
        this.bw = new BufferedWriter(new FileWriter(this.htmlOutputFile));
        writePageHeader();
        writeValidAfterTime();
        writeKnownFlags();
        writeNumberOfRelaysVotedAbout();
        writeConsensusMethods();
        writeRecommendedVersions();
        writeConsensusParameters();
        writeAuthorityKeys();
        writeBandwidthScannerStatus();
        writeAuthorityVersions();
        writeDownloadStatistics();
        writeRelayFlagsSummary();
        writeRelayFlagsTable();
        writePageFooter();
        this.bw.close();
      } catch (IOException e) {
        System.err.println("Could not write HTML output file '"
            + this.htmlOutputFile.getAbsolutePath() + "'.  Ignoring.");
      }
    }
  }

  /* Write the HTML page header including the metrics website
   * navigation. */
  private void writePageHeader() throws IOException {
    this.bw.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 "
          + "Transitional//EN\">\n"
        + "<html>\n"
        + "  <head>\n"
        + "    <title>Consensus health</title>\n"
        + "    <meta http-equiv=\"content-type\" content=\"text/html; "
          + "charset=ISO-8859-1\">\n"
        + "    <link href=\"/css/stylesheet-ltr.css\" type=\"text/css\" "
          + "rel=\"stylesheet\">\n"
        + "    <link href=\"/images/favicon.ico\" "
          + "type=\"image/x-icon\" rel=\"shortcut icon\">\n"
        + "  </head>\n"
        + "  <body>\n"
        + "  <style>\n"
        + "    tr:nth-child(2n) {\n"
        + "      background-color:#eeeeee;\n"
        + "    }\n"
        + "    .oiv {\n"
        + "      color:red;\n"
        + "    }\n"
        + "    .oic {\n"
        + "      color:gray;\n"
        + "      text-decoration:line-through;\n"
        + "    }\n"
        + "    .ic {\n"
        + "      color:blue;\n"
        + "    }\n"
        + "    .tbl-hdr {\n"
        + "      height:3em;\n"
        + "      vertical-align:bottom;\n"
        + "    }\n"
        + "  </style>\n"
        + "    <div class=\"center\">\n"
        + "      <div class=\"main-column\">\n"
        + "        <h2>Consensus Health</h2>\n"
        + "        <br>\n"
        + "        <p>This page shows statistics about the current "
          + "consensus and votes to facilitate debugging of the "
          + "directory consensus process.</p>\n");
  }

  /* Write the valid-after time of the downloaded consensus. */
  private void writeValidAfterTime() throws IOException {
    this.bw.write("        <br>\n"
        + "        <a name=\"validafter\">\n"
        + "        <h3><a href=\"#validafter\" class=\"anchor\">"
          + "Valid-after time</a></h3>\n"
        + "        <br>\n"
        + "        <p>Consensus was published ");
    if (this.downloadedConsensus.getValidAfterMillis() <
        System.currentTimeMillis() - 3L * 60L * 60L * 1000L) {
      this.bw.write("<span class=\"oiv\">"
          + dateTimeFormat.format(
          this.downloadedConsensus.getValidAfterMillis()) + "</span>");
    } else {
      this.bw.write(dateTimeFormat.format(
          this.downloadedConsensus.getValidAfterMillis()));
    }
    this.bw.write(". <i>Note that it takes up to 15 minutes to learn "
        + "about new consensus and votes and process them.</i></p>\n");
  }

  /* Write the lists of known flags. */
  private void writeKnownFlags() throws IOException {
    this.bw.write("        <br>\n"
        + "        <a name=\"knownflags\">\n"
        + "        <h3><a href=\"#knownflags\" class=\"anchor\">Known "
          + "flags</a></h3>\n"
        + "        <br>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"160\">\n"
        + "            <col width=\"640\">\n"
        + "          </colgroup>\n");
    if (this.downloadedVotes.size() < 1) {
      this.bw.write("          <tr><td>(No votes.)</td><td></td></tr>\n");
    } else {
      for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
        this.bw.write("          <tr>\n"
            + "            <td>" + vote.getNickname() + "</td>\n"
            + "            <td>known-flags");
        for (String knownFlag : vote.getKnownFlags()) {
          this.bw.write(" " + knownFlag);
        }
        this.bw.write("</td>\n"
            + "          </tr>\n");
      }
    }
    this.bw.write("          <tr>\n"
        + "            <td class=\"ic\">consensus</td>\n"
        + "            <td class=\"ic\">known-flags");
    for (String knownFlag : this.downloadedConsensus.getKnownFlags()) {
      this.bw.write(" " + knownFlag);
    }
    this.bw.write("</td>\n"
        + "          </tr>\n"
        + "        </table>\n");
  }

  /* Write the number of relays voted about. */
  private void writeNumberOfRelaysVotedAbout() throws IOException {
    this.bw.write("        <br>\n"
        + "        <a name=\"numberofrelays\">\n"
        + "        <h3><a href=\"#numberofrelays\" class=\"anchor\">"
          + "Number of relays voted about</a></h3>\n"
        + "        <br>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"160\">\n"
        + "            <col width=\"320\">\n"
        + "            <col width=\"320\">\n"
        + "          </colgroup>\n");
    if (this.downloadedVotes.size() < 1) {
      this.bw.write("          <tr><td>(No votes.)</td><td></td><td></td>"
            + "</tr>\n");
    } else {
      for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
        int runningRelays = 0;
        for (NetworkStatusEntry entry :
            vote.getStatusEntries().values()) {
          if (entry.getFlags().contains("Running")) {
            runningRelays++;
          }
        }
        this.bw.write("          <tr>\n"
            + "            <td>" + vote.getNickname() + "</td>\n"
            + "            <td>" + vote.getStatusEntries().size()
              + " total</td>\n"
            + "            <td>" + runningRelays + " Running</td>\n"
            + "          </tr>\n");
      }
    }
    int runningRelays = 0;
    for (NetworkStatusEntry entry :
        this.downloadedConsensus.getStatusEntries().values()) {
      if (entry.getFlags().contains("Running")) {
        runningRelays++;
      }
    }
    this.bw.write("          <tr>\n"
        + "            <td class=\"ic\">consensus</td>\n"
        + "            <td/>\n"
        + "            <td class=\"ic\">" + runningRelays
          + " Running</td>\n"
        + "          </tr>\n"
        + "        </table>\n");
  }

  /* Write the supported consensus methods of directory authorities and
   * the resulting consensus method. */
  private void writeConsensusMethods() throws IOException {
    this.bw.write("        <br>\n"
        + "        <a name=\"consensusmethods\">\n"
        + "        <h3><a href=\"#consensusmethods\" class=\"anchor\">"
          + "Consensus methods</a></h3>\n"
        + "        <br>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"160\">\n"
        + "            <col width=\"640\">\n"
        + "          </colgroup>\n");
    if (this.downloadedVotes.size() < 1) {
      this.bw.write("          <tr><td>(No votes.)</td><td></td></tr>\n");
    } else {
      for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
        List<Integer> consensusMethods = vote.getConsensusMethods();
        if (consensusMethods.contains(
            this.downloadedConsensus.getConsensusMethod())) {
          this.bw.write("          <tr>\n"
               + "            <td>" + vote.getNickname() + "</td>\n"
               + "            <td>consensus-methods");
          for (int consensusMethod : consensusMethods) {
            this.bw.write(" " + String.valueOf(consensusMethod));
          }
          this.bw.write("</td>\n"
               + "          </tr>\n");
        } else {
          this.bw.write("          <tr>\n"
              + "            <td><span class=\"oiv\">"
                + vote.getNickname() + "</span></td>\n"
              + "            <td><span class=\"oiv\">"
                + "consensus-methods");
          for (int consensusMethod : consensusMethods) {
            this.bw.write(" " + String.valueOf(consensusMethod));
          }
          this.bw.write("</span></td>\n"
            + "          </tr>\n");
        }
      }
    }
    this.bw.write("          <tr>\n"
        + "            <td class=\"ic\">consensus</td>\n"
        + "            <td class=\"ic\">consensus-method "
          + this.downloadedConsensus.getConsensusMethod()
          + "</td>\n"
        + "          </tr>\n"
        + "        </table>\n");
  }

  /* Write recommended versions. */
  private void writeRecommendedVersions() throws IOException {
    this.bw.write("        <br>\n"
        + "        <a name=\"recommendedversions\">\n"
        + "        <h3><a href=\"#recommendedversions\" class=\"anchor\">"
          + "Recommended versions</a></h3>\n"
        + "        <br>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"160\">\n"
        + "            <col width=\"640\">\n"
        + "          </colgroup>\n");
    if (this.downloadedVotes.size() < 1) {
      this.bw.write("          <tr><td>(No votes.)</td><td></td></tr>\n");
    } else {
      for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
        List<String> voteRecommendedClientVersions =
            vote.getRecommendedClientVersions();
        if (voteRecommendedClientVersions != null) {
          if (downloadedConsensus.getRecommendedClientVersions().equals(
              voteRecommendedClientVersions)) {
            this.bw.write("          <tr>\n"
                + "            <td>" + vote.getNickname() + "</td>\n"
                + "            <td>client-versions ");
            int i = 0;
            for (String version : voteRecommendedClientVersions) {
              this.bw.write((i++ > 0 ? "," : "") + version);
            }
            this.bw.write("</td>\n"
                + "          </tr>\n");
          } else {
            this.bw.write("          <tr>\n"
                + "            <td><span class=\"oiv\">"
                  + vote.getNickname()
                  + "</span></td>\n"
                + "            <td><span class=\"oiv\">client-versions ");
            int i = 0;
            for (String version : voteRecommendedClientVersions) {
              this.bw.write((i++ > 0 ? "," : "") + version);
            }
            this.bw.write("</span></td>\n"
                + "          </tr>\n");
          }
        }
        List<String> voteRecommendedServerVersions =
            vote.getRecommendedServerVersions();
        if (voteRecommendedServerVersions != null) {
          if (downloadedConsensus.getRecommendedServerVersions().equals(
              voteRecommendedServerVersions)) {
            this.bw.write("          <tr>\n"
                + "            <td></td>\n"
                + "            <td>server-versions ");
            int i = 0;
            for (String version : voteRecommendedServerVersions) {
              this.bw.write((i++ > 0 ? "," : "") + version);
            }
            this.bw.write("</td>\n"
                + "          </tr>\n");
          } else {
            this.bw.write("          <tr>\n"
                + "            <td></td>\n"
                + "            <td><span class=\"oiv\">server-versions ");
            int i = 0;
            for (String version : voteRecommendedServerVersions) {
              this.bw.write((i++ > 0 ? "," : "") + version);
            }
            this.bw.write("</span></td>\n"
                + "          </tr>\n");
          }
        }
      }
    }
    this.bw.write("          <tr>\n"
        + "            <td class=\"ic\">consensus</td>\n"
        + "            <td class=\"ic\">client-versions ");
    int i = 0;
    for (String version :
        downloadedConsensus.getRecommendedClientVersions()) {
      this.bw.write((i++ > 0 ? "," : "") + version);
    }
    this.bw.write("</td>\n"
        + "          </tr>\n"
        + "          <tr>\n"
        + "            <td></td>\n"
        + "            <td class=\"ic\">server-versions ");
    i = 0;
    for (String version :
        downloadedConsensus.getRecommendedServerVersions()) {
      this.bw.write((i++ > 0 ? "," : "") + version);
    }
    this.bw.write("</td>\n"
      + "          </tr>\n"
      + "        </table>\n");
  }

  /* Write consensus parameters. */
  private void writeConsensusParameters() throws IOException {
    this.bw.write("        <br>\n"
        + "        <a name=\"consensusparams\">\n"
        + "        <h3><a href=\"#consensusparams\" class=\"anchor\">"
          + "Consensus parameters</a></h3>\n"
        + "        <br>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"160\">\n"
        + "            <col width=\"640\">\n"
        + "          </colgroup>\n");
    if (this.downloadedVotes.size() < 1) {
      this.bw.write("          <tr><td>(No votes.)</td><td></td></tr>\n");
    } else {
      Set<String> validParameters = new HashSet<String>(Arrays.asList(
          ("circwindow,CircuitPriorityHalflifeMsec,refuseunknownexits,"
          + "cbtdisabled,cbtnummodes,cbtrecentcount,cbtmaxtimeouts,"
          + "cbtmincircs,cbtquantile,cbtclosequantile,cbttestfreq,"
          + "cbtmintimeout,cbtinitialtimeout,perconnbwburst,"
          + "perconnbwrate,UseOptimisticData,pb_disablepct,"
          + "UseNTorHandshake,NumNTorsPerTAP").split(",")));
      Map<String, Integer> consensusConsensusParams =
          downloadedConsensus.getConsensusParams();
      for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
        Map<String, Integer> voteConsensusParams =
            vote.getConsensusParams();
        boolean conflictOrInvalid = false;
        if (voteConsensusParams != null) {
          for (Map.Entry<String, Integer> e :
              voteConsensusParams.entrySet()) {
            if (!consensusConsensusParams.containsKey(e.getKey()) ||
                !consensusConsensusParams.get(e.getKey()).equals(
                e.getValue()) ||
                (!validParameters.contains(e.getKey()) &&
                !e.getKey().startsWith("bwauth"))) {
              conflictOrInvalid = true;
              break;
            }
          }
        }
        if (conflictOrInvalid) {
          this.bw.write("          <tr>\n"
              + "            <td><span class=\"oiv\">"
                + vote.getNickname() + "</span></td>\n"
              + "            <td><span class=\"oiv\">params");
          for (Map.Entry<String, Integer> e :
              voteConsensusParams.entrySet()) {
            this.bw.write(" " + e.getKey() + "=" + e.getValue());
          }
          this.bw.write("</span></td>\n"
              + "          </tr>\n");
        } else {
          this.bw.write("          <tr>\n"
              + "            <td>" + vote.getNickname() + "</td>\n"
              + "            <td>params");
          for (Map.Entry<String, Integer> e :
              voteConsensusParams.entrySet()) {
            this.bw.write(" " + e.getKey() + "=" + e.getValue());
          }
          this.bw.write("</td>\n"
              + "          </tr>\n");
        }
      }
    }
    this.bw.write("          <tr>\n"
        + "            <td class=\"ic\">consensus</td>\n"
        + "            <td class=\"ic\">params");
    for (Map.Entry<String, Integer> e :
        this.downloadedConsensus.getConsensusParams().entrySet()) {
      this.bw.write(" " + e.getKey() + "=" + e.getValue());
    }
    this.bw.write("</td>\n"
        + "          </tr>\n"
        + "        </table>\n");
  }

  /* Write authority keys and their expiration dates. */
  private void writeAuthorityKeys() throws IOException {
    this.bw.write("        <br>\n"
        + "        <a name=\"authoritykeys\">\n"
        + "        <h3><a href=\"#authoritykeys\" class=\"anchor\">"
          + "Authority keys</a></h3>\n"
        + "        <br>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"160\">\n"
        + "            <col width=\"640\">\n"
        + "          </colgroup>\n");
    if (this.downloadedVotes.size() < 1) {
      this.bw.write("          <tr><td>(No votes.)</td><td></td></tr>\n");
    } else {
      for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
        long voteDirKeyExpiresMillis = vote.getDirKeyExpiresMillis();
        if (voteDirKeyExpiresMillis - 14L * 24L * 60L * 60L * 1000L <
            System.currentTimeMillis()) {
          this.bw.write("          <tr>\n"
              + "            <td><span class=\"oiv\">"
                + vote.getNickname() + "</span></td>\n"
              + "            <td><span class=\"oiv\">dir-key-expires "
                + dateTimeFormat.format(voteDirKeyExpiresMillis)
                + "</span></td>\n"
              + "          </tr>\n");
        } else {
          this.bw.write("          <tr>\n"
              + "            <td>" + vote.getNickname() + "</td>\n"
              + "            <td>dir-key-expires "
                + dateTimeFormat.format(voteDirKeyExpiresMillis)
                + "</td>\n"
              + "          </tr>\n");
        }
      }
    }
    this.bw.write("        </table>\n"
        + "        <br>\n"
        + "        <p><i>Note that expiration dates of legacy keys are "
          + "not included in votes and therefore not listed here!</i>"
          + "</p>\n");
  }

  /* Write the status of bandwidth scanners and results being contained
   * in votes. */
  private void writeBandwidthScannerStatus() throws IOException {
    this.bw.write("        <br>\n"
         + "        <a name=\"bwauthstatus\">\n"
         + "        <h3><a href=\"#bwauthstatus\" class=\"anchor\">"
           + "Bandwidth scanner status</a></h3>\n"
        + "        <br>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"160\">\n"
        + "            <col width=\"640\">\n"
        + "          </colgroup>\n");
    if (this.downloadedVotes.size() < 1) {
      this.bw.write("          <tr><td>(No votes.)</td><td></td></tr>\n");
    } else {
      for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
        int bandwidthWeights = 0;
        for (NetworkStatusEntry entry : vote.getStatusEntries().values()) {
          if (entry.getMeasured() >= 0L) {
            bandwidthWeights++;
          }
        }
        if (bandwidthWeights > 0) {
          this.bw.write("          <tr>\n"
              + "            <td>" + vote.getNickname() + "</td>\n"
              + "            <td>" + bandwidthWeights
                + " Measured values in w lines</td>\n"
              + "          </tr>\n");
        }
      }
    }
    this.bw.write("        </table>\n");
  }

  /* Write directory authority versions. */
  private void writeAuthorityVersions() throws IOException {
    this.bw.write("        <br>\n"
         + "        <a name=\"authorityversions\">\n"
         + "        <h3><a href=\"#authorityversions\" class=\"anchor\">"
           + "Authority versions</a></h3>\n"
        + "        <br>\n");
    SortedMap<String, String> authorityVersions =
        new TreeMap<String, String>();
    for (NetworkStatusEntry entry :
        this.downloadedConsensus.getStatusEntries().values()) {
      if (entry.getFlags().contains("Authority")) {
        authorityVersions.put(entry.getNickname(), entry.getVersion());
      }
    }
    if (authorityVersions.size() < 1) {
      this.bw.write("          <p>(No relays with Authority flag found.)"
            + "</p>\n");
    } else {
      this.bw.write("        <table border=\"0\" cellpadding=\"4\" "
            + "cellspacing=\"0\" summary=\"\">\n"
          + "          <colgroup>\n"
          + "            <col width=\"160\">\n"
          + "            <col width=\"640\">\n"
          + "          </colgroup>\n");
      for (Map.Entry<String, String> e : authorityVersions.entrySet()) {
        String nickname = e.getKey();
        String versionString = e.getValue();
        this.bw.write("          <tr>\n"
            + "            <td>" + nickname + "</td>\n"
            + "            <td>" + versionString + "</td>\n"
            + "          </tr>\n");
      }
      this.bw.write("        </table>\n"
          + "        <br>\n"
          + "        <p><i>Note that this list of relays with the "
            + "Authority flag may be different from the list of v3 "
            + "directory authorities!</i></p>\n");
    }
  }


  /* Write some download statistics. */
  private void writeDownloadStatistics() throws IOException {
    SortedSet<String> knownAuthorities =
        this.statistics.getKnownAuthorities();
    if (knownAuthorities.isEmpty()) {
      return;
    }
    this.bw.write("        <br>\n"
         + "        <a name=\"downloadstats\">\n"
         + "        <h3><a href=\"#downloadstats\" class=\"anchor\">"
           + "Consensus download statistics</a></h3>\n"
        + "        <br>\n"
        + "        <p>The following table contains statistics on "
          + "consensus download times in milliseconds over the last 7 "
          + "days:</p>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"160\">\n"
        + "            <col width=\"100\">\n"
        + "            <col width=\"100\">\n"
        + "            <col width=\"100\">\n"
        + "            <col width=\"100\">\n"
        + "            <col width=\"100\">\n"
        + "            <col width=\"100\">\n"
        + "          </colgroup>\n"
        + "          <tr><th>Authority</th>"
          + "<th>Minimum</th>"
          + "<th>1st Quartile</th>"
          + "<th>Median</th>"
          + "<th>3rd Quartile</th>"
          + "<th>Maximum</th>"
          + "<th>Timeouts</th></tr>\n");
    for (String authority : knownAuthorities) {
      this.bw.write("          <tr>\n"
          + "            <td>" + authority + "</td>\n"
          + "            <td>"
            + this.statistics.getPercentile(authority, 0) + "</td>"
          + "            <td>"
            + this.statistics.getPercentile(authority, 25) + "</td>"
          + "            <td>"
            + this.statistics.getPercentile(authority, 50) + "</td>"
          + "            <td>"
            + this.statistics.getPercentile(authority, 75) + "</td>"
          + "            <td>"
            + this.statistics.getPercentile(authority, 100) + "</td>"
          + "            <td>"
            + this.statistics.getNAs(authority) + "</td></tr>\n");
    }
    this.bw.write("        </table>\n");
  }

  /* Write the (huge) table containing relay flags contained in votes and
   * the consensus for each relay. */
  private void writeRelayFlagsTable() throws IOException {
    this.bw.write("        <br>\n"
        + "        <a name=\"relayflags\">\n"
        + "        <h3><a href=\"#relayflags\" class=\"anchor\">Relay "
          + "flags</a></h3>\n"
        + "        <br>\n"
        + "        <p>The semantics of flags written in the table is "
          + "similar to the table above:</p>\n"
        + "        <ul>\n"
        + "          <li><b>In vote and consensus:</b> Flag in vote "
          + "matches flag in consensus, or relay is not listed in "
          + "consensus (because it doesn't have the Running "
          + "flag)</li>\n"
        + "          <li><b><span class=\"oiv\">Only in "
          + "vote:</span></b> Flag in vote, but missing in the "
          + "consensus, because there was no majority for the flag or "
          + "the flag was invalidated (e.g., Named gets invalidated by "
          + "Unnamed)</li>\n"
        + "          <li><b><span class=\"oic\">Only in "
          + "consensus:</span></b> Flag in consensus, but missing "
          + "in a vote of a directory authority voting on this "
          + "flag</li>\n"
        + "          <li><b><span class=\"ic\">In "
          + "consensus:</span></b> Flag in consensus</li>\n"
        + "        </ul>\n"
        + "        <br>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"120\">\n"
        + "            <col width=\"80\">\n");
    for (int i = 0; i < this.downloadedVotes.size(); i++) {
      this.bw.write("            <col width=\""
          + (640 / this.downloadedVotes.size()) + "\">\n");
    }
    this.bw.write("          </colgroup>\n");
    SortedMap<String, String> allRelays = new TreeMap<String, String>();
    for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
      for (NetworkStatusEntry statusEntry :
          vote.getStatusEntries().values()) {
        allRelays.put(statusEntry.getFingerprint(),
            statusEntry.getNickname());
      }
    }
    for (NetworkStatusEntry statusEntry :
        this.downloadedConsensus.getStatusEntries().values()) {
      allRelays.put(statusEntry.getFingerprint(),
          statusEntry.getNickname());
    }
    int linesWritten = 0;
    for (Map.Entry<String, String> e : allRelays.entrySet()) {
      if (linesWritten++ % 10 == 0) {
        this.writeRelayFlagsTableHeader();
      }
      String fingerprint = e.getKey();
      String nickname = e.getValue();
      this.writeRelayFlagsTableRow(fingerprint, nickname);
    }
    this.bw.write("        </table>\n");
  }

  /* Write the table header that is repeated every ten relays and that
   * contains the directory authority names. */
  private void writeRelayFlagsTableHeader() throws IOException {
    this.bw.write("          <tr class=\"tbl-hdr\"><th>Fingerprint</th>"
        + "<th>Nickname</th>\n");
    for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
      String shortDirName = vote.getNickname().length() > 6 ?
          vote.getNickname().substring(0, 5) + "." :
          vote.getNickname();
      this.bw.write("<th>" + shortDirName + "</th>");
    }
    this.bw.write("<th>consensus</th></tr>\n");
  }

  /* Write a single row in the table of relay flags. */
  private void writeRelayFlagsTableRow(String fingerprint,
      String nickname) throws IOException {
    this.bw.write("          <tr>\n");
    if (this.downloadedConsensus.containsStatusEntry(fingerprint) &&
        this.downloadedConsensus.getStatusEntry(fingerprint).getFlags().
        contains("Named") && !Character.isDigit(nickname.charAt(0))) {
      this.bw.write("            <td id=\"" + nickname + "\">"
          + fingerprint.substring(0, 8) + "</td>\n");
    } else {
      this.bw.write("            <td>"
          + fingerprint.substring(0, 8) + "</td>\n");
    }
    this.bw.write("            <td>" + nickname + "</td>\n");
    SortedSet<String> relevantFlags = new TreeSet<String>();
    for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
      if (vote.containsStatusEntry(fingerprint)) {
        relevantFlags.addAll(vote.getStatusEntry(fingerprint).getFlags());
      }
    }
    SortedSet<String> consensusFlags = null;
    if (this.downloadedConsensus.containsStatusEntry(fingerprint)) {
      consensusFlags = this.downloadedConsensus.getStatusEntries().get(
          fingerprint).getFlags();
      relevantFlags.addAll(consensusFlags);
    }
    for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
      if (vote.containsStatusEntry(fingerprint)) {
        SortedSet<String> flags = vote.getStatusEntry(fingerprint).
            getFlags();
        this.bw.write("            <td>");
        int flagsWritten = 0;
        for (String flag : relevantFlags) {
          this.bw.write(flagsWritten++ > 0 ? "<br>" : "");
          if (flags.contains(flag)) {
            if (consensusFlags == null ||
              consensusFlags.contains(flag)) {
              this.bw.write(flag);
            } else {
              this.bw.write("<span class=\"oiv\">" + flag + "</span>");
            }
          } else if (consensusFlags != null &&
              vote.getKnownFlags().contains(flag) &&
              consensusFlags.contains(flag)) {
            this.bw.write("<span class=\"oic\">" + flag
                + "</span>");
          }
        }
        this.bw.write("</td>\n");
      } else {
        this.bw.write("            <td></td>\n");
      }
    }
    if (consensusFlags != null) {
      this.bw.write("            <td class=\"ic\">");
      int flagsWritten = 0;
      for (String flag : relevantFlags) {
        this.bw.write(flagsWritten++ > 0 ? "<br>" : "");
        if (consensusFlags.contains(flag)) {
          this.bw.write(flag);
        }
      }
      this.bw.write("</td>\n");
    } else {
      this.bw.write("            <td></td>\n");
    }
    this.bw.write("          </tr>\n");
  }

  /* Write the relay flag summary. */
  private void writeRelayFlagsSummary() throws IOException {
    this.bw.write("        <br>\n"
        + "        <a name=\"overlap\">\n"
        + "        <h3><a href=\"#overlap\" class=\"anchor\">Overlap "
          + "between votes and consensus</a></h3>\n"
        + "        <br>\n"
        + "        <p>The semantics of columns is as follows:</p>\n"
        + "        <ul>\n"
        + "          <li><b>In vote and consensus:</b> Flag in vote "
          + "matches flag in consensus, or relay is not listed in "
          + "consensus (because it doesn't have the Running "
          + "flag)</li>\n"
        + "          <li><b><span class=\"oiv\">Only in "
          + "vote:</span></b> Flag in vote, but missing in the "
          + "consensus, because there was no majority for the flag or "
          + "the flag was invalidated (e.g., Named gets invalidated by "
          + "Unnamed)</li>\n"
        + "          <li><b><span class=\"oic\">Only in "
          + "consensus:</span></b> Flag in consensus, but missing "
          + "in a vote of a directory authority voting on this "
          + "flag</li>\n"
        + "        </ul>\n"
        + "        <br>\n"
        + "        <table border=\"0\" cellpadding=\"4\" "
        + "cellspacing=\"0\" summary=\"\">\n"
        + "          <colgroup>\n"
        + "            <col width=\"160\">\n"
        + "            <col width=\"210\">\n"
        + "            <col width=\"210\">\n"
        + "            <col width=\"210\">\n"
        + "          </colgroup>\n"
        + "          <tr><td></td><td><b>Only in vote</b></td>"
          + "<td><b>In vote and consensus</b></td>"
          + "<td><b>Only in consensus</b></td>\n");
    Set<String> allFingerprints = new HashSet<String>();
    for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
      allFingerprints.addAll(vote.getStatusEntries().keySet());
    }
    allFingerprints.addAll(this.downloadedConsensus.getStatusEntries().
        keySet());
    SortedMap<String, SortedMap<String, Integer>> flagsAgree =
        new TreeMap<String, SortedMap<String, Integer>>();
    SortedMap<String, SortedMap<String, Integer>> flagsLost =
        new TreeMap<String, SortedMap<String, Integer>>();
    SortedMap<String, SortedMap<String, Integer>> flagsMissing =
        new TreeMap<String, SortedMap<String, Integer>>();
    for (String fingerprint : allFingerprints) {
      SortedSet<String> consensusFlags =
          this.downloadedConsensus.containsStatusEntry(fingerprint) ?
          this.downloadedConsensus.getStatusEntry(fingerprint).getFlags()
          : null;
      for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
        String dir = vote.getNickname();
        if (vote.containsStatusEntry(fingerprint)) {
          SortedSet<String> flags = vote.getStatusEntry(fingerprint).
              getFlags();
          for (String flag : this.downloadedConsensus.getKnownFlags()) {
            SortedMap<String, SortedMap<String, Integer>> sums = null;
            if (flags.contains(flag)) {
              if (consensusFlags == null ||
                consensusFlags.contains(flag)) {
                sums = flagsAgree;
              } else {
                sums = flagsLost;
              }
            } else if (consensusFlags != null &&
                vote.getKnownFlags().contains(flag) &&
                consensusFlags.contains(flag)) {
              sums = flagsMissing;
            }
            if (sums != null) {
              SortedMap<String, Integer> sum = null;
              if (sums.containsKey(dir)) {
                sum = sums.get(dir);
              } else {
                sum = new TreeMap<String, Integer>();
                sums.put(dir, sum);
              }
              sum.put(flag, sum.containsKey(flag) ?
                  sum.get(flag) + 1 : 1);
            }
          }
        }
      }
    }
    for (RelayNetworkStatusVote vote : this.downloadedVotes.values()) {
      String dir = vote.getNickname();
      int i = 0;
      for (String flag : vote.getKnownFlags()) {
        this.bw.write("          <tr>\n"
            + "            <td>" + (i++ == 0 ? dir : "")
              + "</td>\n");
        if (flagsLost.containsKey(dir) &&
            flagsLost.get(dir).containsKey(flag)) {
          this.bw.write("            <td><span class=\"oiv\"> "
                + flagsLost.get(dir).get(flag) + " " + flag
                + "</span></td>\n");
        } else {
          this.bw.write("            <td></td>\n");
        }
        if (flagsAgree.containsKey(dir) &&
            flagsAgree.get(dir).containsKey(flag)) {
          this.bw.write("            <td>" + flagsAgree.get(dir).get(flag)
                + " " + flag + "</td>\n");
        } else {
          this.bw.write("            <td></td>\n");
        }
        if (flagsMissing.containsKey(dir) &&
            flagsMissing.get(dir).containsKey(flag)) {
          this.bw.write("            <td><span class=\"oic\">"
                + flagsMissing.get(dir).get(flag) + " " + flag
                + "</span></td>\n");
        } else {
          this.bw.write("            <td></td>\n");
        }
        this.bw.write("          </tr>\n");
      }
    }
    this.bw.write("        </table>\n");
  }

  /* Write the footer of the HTML page containing the blurb that is on
   * every page of the metrics website. */
  private void writePageFooter() throws IOException {
    this.bw.write("      </div>\n"
        + "    </div>\n"
        + "    <div class=\"bottom\" id=\"bottom\">\n"
        + "      <p>\"Tor\" and the \"Onion Logo\" are <a "
          + "href=\"https://www.torproject.org/docs/trademark-faq.html"
          + ".en\">"
        + "registered trademarks</a> of The Tor Project, "
          + "Inc.</p>\n"
        + "    </div>\n"
        + "  </body>\n"
        + "</html>");
  }
}

