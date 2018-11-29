package edu.unl.cse.efs.view.tcselect;

import edu.unl.cse.efs.generate.DirectionalPack;

public class IPSelectorUserState {
	public boolean bidirectional;
	public boolean rightDirection;

	public IPSelectorUserState(DirectionalPack dp)
	{
		if(dp.isBidirectional())
			bidirectional = true;
		else if(dp.isRightDirectional())
			rightDirection = true;
	}
}
