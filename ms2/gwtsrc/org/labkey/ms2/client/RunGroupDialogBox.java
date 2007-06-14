package org.labkey.ms2.client;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;

import org.labkey.api.gwt.client.ui.WindowUtil;
import org.labkey.api.gwt.client.ui.ImageButton;

/**
 * User: jeckels
 * Date: Jun 12, 2007
 */
public abstract class RunGroupDialogBox extends DialogBox
{
    private CheckBox[] _runCheckBoxes;
    private RadioButton _requireAllRadioButton;
    private RadioButton _requireAnyRadioButton;


    public RunGroupDialogBox(String[] runNames, RunGroup runGroup)
    {
        super(false);
        setText("Select the runs to be included in the group");

        VerticalPanel rootPanel = new VerticalPanel();
        rootPanel.setSpacing(10);

        KeyboardListener listener = new KeyboardListenerAdapter()
        {
            public void onKeyDown(Widget sender, char keyCode, int modifiers)
            {
                if (keyCode == KeyboardListener.KEY_ESCAPE)
                {
                    hide();
                }
            }
        };

        Grid grid = new Grid();
        grid.resize(runNames.length, 1);
        _runCheckBoxes = new CheckBox[runNames.length];
        for (int i = 0; i < runNames.length; i++)
        {
            _runCheckBoxes[i] = new CheckBox(runNames[i]);
            _runCheckBoxes[i].addKeyboardListener(listener);
            if (runGroup.containsRunIndex(i))
            {
                _runCheckBoxes[i].setChecked(true);
            }
            grid.setWidget(i, 0, _runCheckBoxes[i]);
        }

        rootPanel.add(grid);

        _requireAllRadioButton = new RadioButton("requireAll", "Only include proteins indentified in ALL runs in this group");
        _requireAllRadioButton.setChecked(runGroup.isRequireAll());
        rootPanel.add(_requireAllRadioButton);

        _requireAnyRadioButton = new RadioButton("requireAll", "Include proteins identified in ANY of the runs in this group");
        _requireAnyRadioButton.setChecked(!runGroup.isRequireAll());
        rootPanel.add(_requireAnyRadioButton);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setSpacing(5);

        ImageButton okButton = new ImageButton("OK");
        buttonPanel.add(okButton);
        okButton.addClickListener(new ClickListener()
        {
            public void onClick(Widget sender)
            {
                int count = 0;
                for (int i = 0; i < _runCheckBoxes.length; i++)
                {
                    if (_runCheckBoxes[i].isChecked())
                    {
                        count++;
                    }
                }

                if (count == 0)
                {
                    Window.alert("Each group must contain at least one run.");
                    return;
                }

                int[] result = new int[count];
                int index = 0;
                for (int i = 0; i < _runCheckBoxes.length; i++)
                {
                    if (_runCheckBoxes[i].isChecked())
                    {
                        result[index++] = i;
                    }
                }

                commit(new RunGroup(result, _requireAllRadioButton.isChecked()));
                hide();
            }
        });

        ImageButton cancelButton = new ImageButton("Cancel");
        cancelButton.addClickListener(new ClickListener()
        {
            public void onClick(Widget sender)
            {
                hide();
            }
        });
        buttonPanel.add(cancelButton);
        rootPanel.add(buttonPanel);

        setWidget(rootPanel);
    }

    public void show()
    {
        WindowUtil.centerDialog(this);
        super.show();
        WindowUtil.centerDialog(this);
    }

    public abstract void commit(RunGroup runGroup);
}
