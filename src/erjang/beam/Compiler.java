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

package erjang.beam;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ericsson.otp.erlang.OtpAuthException;
import com.sun.tools.javac.resources.compiler;

import erjang.EModule;
import erjang.EObject;
import erjang.ErlFun;
import erjang.Module;
import erjang.beam.analysis.BeamTypeAnalysis;

public class Compiler implements Opcodes {

	static ErlangBeamDisLoader loader;

	/**
	 * @throws IOException
	 * @throws OtpAuthException
	 * 
	 */
	public Compiler() throws OtpAuthException, IOException {
		if (loader == null)
			loader = new ErlangBeamDisLoader();
	}

	byte[] compile(File file) throws IOException {
		// class writer, phase 4
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		// the java bytecode generator, phase 3
		CompilerVisitor cv = new CompilerVisitor(cw);

		// the type analysis, phase 2
		BeamTypeAnalysis analysis = new BeamTypeAnalysis(cv);

		// the beam file reader, phase 1
		BeamFileData reader = loader.load(file);

		// go!
		reader.accept(analysis);

		// get byte code data
		return cw.toByteArray();
	}

	public static void main(String[] args) throws Exception {

		File out_dir = new File("out");
		Compiler cc = new Compiler();
		
		for (int i = 0; i < args.length; i++) {
			
			File infile = new File(args[i]);
			byte[] data = cc.compile(infile);
			String beam_name = infile.getName();
			String jbeam_name = beam_name.substring(0, beam_name.lastIndexOf('.')+1) + "jbeam";
			File outfile = new File(out_dir, beam_name);
			writeTo(outfile , data);
		}

		
	}

	static void writeTo(File output, byte[] class_data) throws IOException {

		FileOutputStream fo = new FileOutputStream(output);
		try {
			fo.write(class_data);
		} finally {
			if (fo != null)
				fo.close();
		}

	}

}