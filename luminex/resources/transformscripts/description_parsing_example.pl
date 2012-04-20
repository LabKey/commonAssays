#
# Program:   description_parsing_exmaple.pl
# Creation date: 13Apr2012
# Programmer:  Cory Nathe
#
# Description: an example transform script for parsing the Description field
#              in the Luminex assay upload files into participantID, visitID,
#              and date. Two example parsings are supported in this script:
#              121410073 4 2009-08-10
#              P562, Wk 48, 07-27-2011
#
# Date Revised  Revised by  Description
# ------------  ----------  ----------------------------------------
#
#

use strict;
use warnings;
use LWP::UserAgent;

# open the run properties file
open my $runProps, '<', '${runInfo}';

my $runInputFile = "temp";
my $runOutputFile = "temp";
while (<$runProps>) {
   chomp;
   my @row = split /\t/;
   
   if ($row[0] eq 'runDataFile') {
	$runInputFile = $row[1];
	$runOutputFile = $row[3];
   }
}

close $runProps;

open my $myInput, '<', "$runInputFile" or die "Can't open $runInputFile: $!\n";
open my $myOutput, '>', "$runOutputFile" or die "Can't open $runOutputFile:$!\n";

# get the column headers
my $runheader = <$myInput>; 
chomp $runheader;
my @runheader = split /\t/, $runheader;
my %runColNames = map { $runheader[$_], $_ } 0..$#runheader;

# print the column headers to the output file
$" = "\t";
print {$myOutput} "@runheader\n";

my $descindex=$runColNames{'description'};
my $ptidindex=$runColNames{'participantID'};
my $visitindex=$runColNames{'visitID'};
my $dateindex=$runColNames{'date'};
my $typeindex=$runColNames{'type'};

# parse the description field for ptid, visitId, and date information
while (<$myInput>){
   chomp;
   my @mydata = split /\t/, $_;
   my $desc = $mydata[$descindex];
   my $type = $mydata[$typeindex];

   if ( substr($type,0,1) eq "X" ) {
      
      #Format: 123456789 4 2009-08-10
      #        123456789 10 2009-08-10
      if ( $desc =~ /^([0-9]{9}) ([0-9]{1,2}) ([0-9]{4}-[0-9]{2}-[0-9]{2})$/ ) {
         $mydata[$ptidindex] = $1;
         $mydata[$visitindex] = $2;
         $mydata[$dateindex] = $3;
      }
      
      #Format: P123, Wk 48, 07-27-2011
      #        123456789, Wk 8, 07-27-2011
      #        T2345, D 1, 07-27-2011
      if ( $desc =~ /^([A-Z0-9]{4,9}), (Wk|D) ([0-9]{1,2}), ([0-9]{1,2}-[0-9]{2}-[0-9]{4})$/ ) {
         $mydata[$ptidindex] = $1;
         $mydata[$visitindex] = $3;
         $mydata[$dateindex] = $4;
      }
      
   }

   # print the data row to the output file   
   $" = "\t";
   print {$myOutput} "@mydata\n";
}

close $myInput;
close $myOutput;

exit 0;

