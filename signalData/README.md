# SignalData

## Basic Setup ##

1. Create a LabKey Assay Project/Folder
  1. Hover over the Project or Folder menu, and click **Create Project** or **Create Subfolder**.
  1. Provide a Name and select type **Assay**.
  1. Click **Next**.
  1. Set permissions (optional) and click **Next**.
  1. Click **Finish**.
1. Create a Signal Data Assay Design
  1.  In the **Assay List** web part, click **New Assay Design**.
  1.  On the New Assay Design page, select **Signal Data** as the assay type.
  1.  Click **Next**.
  1.  Name is the only required field. (Design default values should work fine.)
  1.  Click the **Save & Close** button.

## Manual Import of Signal Data Results ##

Importing data is a two step process:

1. Upload the metadata file. (An example metadata file is available at: https://github.com/LabKey/signalData/blob/develop/test/sampledata/signaldata/RunsMetadata/datafiles.tsv)
1. Drag and drop the data files (Example data files are available at: https://github.com/LabKey/signalData/tree/develop/test/sampledata/signaldata/SignalDataAssayData/TestRun001)

### Metadata Upload

1. Go to the folder where you wish to upload results.
1. Add the **Signal Data Upload** webpart.
1. Confirm that **File** is selected, and click **Upload TXT, TSV, XLS, XLSX**.

![Drag-and-drop to upload a run](https://github.com/LabKey/docs/blob/master/images/signal1.png?raw=true)

1. Specify a Run Identifier (required).

![Drag-and-drop to upload a run](https://github.com/LabKey/docs/blob/master/images/signal2.png?raw=true)

### Data Upload

1. Drag-and-drop any result files you want to import onto the dropzone.

![Drag-and-drop to upload a run](https://github.com/LabKey/docs/blob/master/images/signal3.png?raw=true)

1. Click the **Save Run** button.

![Name and save a run](https://github.com/LabKey/docs/blob/master/images/signal4.png?raw=true)

## View Results ##

To view chromatograms for the results, follow these instructions:

1. Go to the folder where your results reside.
1. On the **Assay List** webpart, click the name of the Signal Data assay you created.
1. Select the run(s) you want to look at, using the checkboxes.
1. Click **View Selected Runs**.

![View runs](https://github.com/LabKey/docs/blob/master/images/hplc3.png?raw=true) 

1. Select the data run(s) you wish to view from the left.
1. Click the `Overlay Selected` button.

![View runs](https://github.com/LabKey/docs/blob/master/images/hplc4.png?raw=true) 

1. The left selection pane will close, and the overlayed results will be displayed in the center pane.

![View runs](https://github.com/LabKey/docs/blob/master/images/hplc5.png?raw=true) 

1. You can zoom into the graph by entering values in the **Time** and **mV Range** text boxes, or by drawing a new area inside the graph using the mouse.

![View runs](https://github.com/LabKey/docs/blob/master/images/hplc6.png?raw=true) 

Enter values in the right pane to calculate areas under the curves. Select in the name column to highlight different graph lines.

![View runs](https://github.com/LabKey/docs/blob/master/images/hplc7.png?raw=true) 

## Installing the Signal Data Module ##

[Contact us](https://www.labkey.com/company/contact-us) for details on installing the Signal Data module.


