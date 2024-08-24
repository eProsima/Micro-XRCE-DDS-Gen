// Copyright 2016 Proyectos y Sistemas de Mantenimiento SL (eProsima).
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.eprosima.microxrcedds.idl.parser.typecode;

import com.eprosima.idl.parser.typecode.Member;
import com.eprosima.idl.parser.tree.Annotation;

public class SequenceTypeCode extends com.eprosima.idl.parser.typecode.SequenceTypeCode
{
    public SequenceTypeCode(
            String maxsize,
            String evaluated_maxsize,
            String default_unbounded_max_size_param)
    {
        super(maxsize, evaluated_maxsize);
        default_unbounded_max_size = default_unbounded_max_size_param;
    }

}
