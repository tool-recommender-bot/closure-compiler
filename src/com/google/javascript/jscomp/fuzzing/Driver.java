/*
 * Copyright 2013 The Closure Compiler Authors.
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
 */
package com.google.javascript.jscomp.fuzzing;

import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerInput;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.SyntheticAst;
import com.google.javascript.rhino.Node;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * UNDER DEVELOPMENT. DO NOT USE!
 * @author zplin@google.com (Zhongpeng Lin)
 */
public class Driver {
  @Option(name = "--number_of_runs",
      usage = "The number of runs of the fuzzer. Default: 1")
  private int numberOfRuns = 1;

  @Option(name = "--max_ast_size",
      usage = "The max number of nodes in the generated ASTs. Default: 100")
  private int maxASTSize = 100;

  @Option(name = "--compilation_level",
      usage = "Specifies the compilation level to use. Options: " +
      "WHITESPACE_ONLY, SIMPLE_OPTIMIZATIONS, ADVANCED_OPTIMIZATIONS. " +
      "Default: SIMPLE_OPTIMIZATIONS")
  private CompilationLevel compilationLevel =
      CompilationLevel.SIMPLE_OPTIMIZATIONS;

  public Result compile(String code) throws IOException {
    Compiler compiler = new Compiler();
    return compiler.compile(CommandLineRunner.getDefaultExterns(),
        Arrays.asList(SourceFile.fromCode("[fuzzedCode]", code)), getOptions());
  }

  public Result compile(Node script) throws IOException {
    CompilerInput input = new CompilerInput(new SyntheticAst(script));
    JSModule jsModule = new JSModule("fuzzedModule");
    jsModule.add(input);

    Compiler compiler = new Compiler();
    return compiler.compileModules(
        CommandLineRunner.getDefaultExterns(),
        Arrays.asList(jsModule), getOptions());
  }

  private CompilerOptions getOptions() {
    CompilerOptions options = new CompilerOptions();
    compilationLevel.setOptionsForCompilationLevel(options);
    return options;
  }

  public static void main(String[] args) throws IOException {
    Driver driver = new Driver();
    CmdLineParser parser = new CmdLineParser(driver);
    try {
      parser.parseArgument(args);
      for (int i = 0; i < driver.numberOfRuns; i++) {
        long seed = System.currentTimeMillis();
        Random random = new Random(seed);
        System.out.println("Seed: " + seed);
        Fuzzer fuzzer = new Fuzzer(random);
        Node[] nodes = fuzzer.generateProgram(driver.maxASTSize);
        Node script = Fuzzer.buildScript(nodes);
        String code = Fuzzer.getPrettyCode(script);
        System.out.println(code.trim());

        Result result = driver.compile(script);
        if (result.success) {
          System.out.println("Success! [" + i + " of " + driver.numberOfRuns +
              "]\n");
        }
      }
    } catch (CmdLineException e) {
      // handling of wrong arguments
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }
}
