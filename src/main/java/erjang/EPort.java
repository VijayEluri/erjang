/**
 * This file is part of Erjang - A JVM-based Erlang VM
 *
 * Copyright (c) 2009 by Trifork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package erjang;

public class EPort extends EObject {

	/* (non-Javadoc)
	 * @see erjang.EObject#cmpOrder()
	 */
	@Override
	int cmp_order() {
		return 4;
	}
	/**
	 * @return
	 */
	public EString getName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	int compare_same(EObject rhs) {
		// TODO: make faster
		return toString().compareTo(rhs.toString());
	}
	


}
