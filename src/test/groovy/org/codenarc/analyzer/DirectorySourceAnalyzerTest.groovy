/*
 * Copyright 2008 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codenarc.analyzer

import org.codenarc.results.DirectoryResults
import org.codenarc.results.FileResults
import org.codenarc.results.Results
import org.codenarc.rule.TestCountRule
import org.codenarc.rule.TestPathRule
import org.codenarc.ruleset.ListRuleSet
import org.codenarc.test.AbstractTest

/**
 * Tests for DirectorySourceAnalyzer.
 *
 * @author Chris Mair
 * @version $Revision$ - $Date$
 */
class DirectorySourceAnalyzerTest extends AbstractTest {
    static final BASE_DIR = '/usr'
    def analyzer
    def ruleSet
    def testCountRule

    void testAnalyze_NullRuleSet() {
        analyzer.baseDirectory = BASE_DIR
        shouldFailWithMessageContaining('ruleSet') { analyzer.analyze(null) }
    }

    void testAnalyze_BaseDirectoryNullAndSourceDirectoriesNull() {
        shouldFailWithMessageContaining(['baseDirectory','sourceDirectories']) { analyzer.analyze(ruleSet) }
    }

    void testAnalyze_BaseDirectoryEmptyAndSourceDirectoriesEmpty() {
        analyzer.baseDirectory = ''
        analyzer.sourceDirectories = []
        shouldFailWithMessageContaining(['baseDirectory','sourceDirectories']) { analyzer.analyze(ruleSet) }
    }

    void testAnalyze_BaseDirectory_FilesOnly() {
        final DIR = 'src/test/resources/source'
        analyzer.baseDirectory = DIR
        def ruleSet = new ListRuleSet([new TestPathRule()])
        def results = analyzer.analyze(ruleSet)
        log("results=$results")

        def paths = resultsPaths(results)
        log("paths=$paths")
        assertEqualSets(paths, ["SourceFile1.groovy", "SourceFile2.groovy"])

        def fullPaths = results.getViolationsWithPriority(1).collect { it.message }
        assert fullPaths == [
                'src/test/resources/source/SourceFile1.groovy',
                'src/test/resources/source/SourceFile2.groovy'
        ]
        assert results.numberOfFilesWithViolations == 2
        assert results.totalNumberOfFiles == 2
    }

    void testAnalyze_BaseDirectory() {
        final DIR = 'src/test/resources/sourcewithdirs'
        analyzer.baseDirectory = DIR
        def results = analyzer.analyze(ruleSet)
        log("results=$results")

        def fullPaths = results.getViolationsWithPriority(1).collect { it.message }
        assert fullPaths == [
                'src/test/resources/sourcewithdirs/SourceFile1.groovy',
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File1.groovy',
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File2.groovy',
                'src/test/resources/sourcewithdirs/subdir2/subdir2a/Subdir2aFile1.groovy',
                'src/test/resources/sourcewithdirs/subdir2/Subdir2File1.groovy'
        ]
        assert testCountRule.count == 5
        assert results.numberOfFilesWithViolations == 5
        assert results.totalNumberOfFiles == 5

        // Verify that the directory structure is properly reflected within the results
        assert childResultsClasses(results) == [DirectoryResults]
        def top = results.children[0]
        assert childResultsClasses(top) == [FileResults, DirectoryResults, DirectoryResults]
        assert childResultsClasses(top.children[1]) == [FileResults, FileResults]
        assert childResultsClasses(top.children[2]) == [DirectoryResults, FileResults]
        assert childResultsClasses(top.children[2].children[0]) == [FileResults]
    }

    void testAnalyze_SourceDirectories() {
        final DIR1 = 'src/test/resources/source'
        final DIR2 = 'src/test/resources/sourcewithdirs'
        analyzer.sourceDirectories = [DIR1, DIR2]
        def results = analyzer.analyze(ruleSet)
        log("results=$results")
        def fullPaths = results.getViolationsWithPriority(1).collect { it.message }
        log("fullPaths=$fullPaths")
        assert fullPaths == [
                'src/test/resources/source/SourceFile1.groovy',
                'src/test/resources/source/SourceFile2.groovy',
                'src/test/resources/sourcewithdirs/SourceFile1.groovy',
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File1.groovy',
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File2.groovy',
                'src/test/resources/sourcewithdirs/subdir2/subdir2a/Subdir2aFile1.groovy',
                'src/test/resources/sourcewithdirs/subdir2/Subdir2File1.groovy'
        ]
        assert testCountRule.count == 7
        assert results.totalNumberOfFiles == 7
        assert results.numberOfFilesWithViolations == 7
    }

    void testAnalyze_BaseDirectoryAndSourceDirectories() {
        final SOURCE_DIRS = ['source', 'sourcewithdirs', 'rulesets']
        analyzer.baseDirectory = 'src/test/resources'
        analyzer.sourceDirectories = SOURCE_DIRS
        def results = analyzer.analyze(ruleSet)

        def paths = resultsPaths(results)
        log("paths=$paths")

        assert paths == [
                'source',
                'source/SourceFile1.groovy',
                'source/SourceFile2.groovy',
                'sourcewithdirs',
                'sourcewithdirs/SourceFile1.groovy',
                'sourcewithdirs/subdir1',
                'sourcewithdirs/subdir1/Subdir1File1.groovy',
                'sourcewithdirs/subdir1/Subdir1File2.groovy',
                'sourcewithdirs/subdir2',
                'sourcewithdirs/subdir2/subdir2a',
                'sourcewithdirs/subdir2/subdir2a/Subdir2aFile1.groovy',
                'sourcewithdirs/subdir2/Subdir2File1.groovy',
                'rulesets',
                'rulesets/GroovyRuleSet1.groovy'
        ]
        assert testCountRule.count == 8
        assert childResultsClasses(results) == [DirectoryResults, DirectoryResults, DirectoryResults]
        assert results.totalNumberOfFiles == 8
        assert results.numberOfFilesWithViolations == 8
    }

    void testAnalyze_BaseDirectory_NoViolations() {
        final DIR = 'src/test/resources/sourcewithdirs'
        analyzer.baseDirectory = DIR
        ruleSet = new ListRuleSet([testCountRule])
        def results = analyzer.analyze(ruleSet)
        log("results=$results")

        def paths = resultsPaths(results)
        log("paths=$paths")
        assertEqualSets(paths, ["subdir1", "subdir2", "subdir2/subdir2a"])

        assert testCountRule.count == 5
        assert results.numberOfFilesWithViolations == 0
        assert results.totalNumberOfFiles == 5
    }

    void testAnalyze_BaseDirectory_ApplyToFilesMatching() {
        final DIR = 'src/test/resources/sourcewithdirs'
        analyzer.baseDirectory = DIR
        analyzer.applyToFilesMatching = /.*ubdir.*\.groovy/
        analyzer.doNotApplyToFilesMatching = /.*File2.*/
        def results = analyzer.analyze(ruleSet)
        log("results=$results")

        def fullPaths = results.getViolationsWithPriority(1).collect { it.message }
        assert fullPaths == [
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File1.groovy',
                'src/test/resources/sourcewithdirs/subdir2/subdir2a/Subdir2aFile1.groovy',
                'src/test/resources/sourcewithdirs/subdir2/Subdir2File1.groovy'
        ]

        assert testCountRule.count == 3
        assert results.numberOfFilesWithViolations == 3
        assert results.totalNumberOfFiles == 3
    }

    void testAnalyze_BaseDirectory_ApplyToFileNames() {
        final DIR = 'src/test/resources/sourcewithdirs'
        analyzer.baseDirectory = DIR
        analyzer.applyToFileNames = 'Subdir1File1.groovy,Subdir2a*1.groovy,Sub?ir2File1.groovy'
        analyzer.doNotApplyToFileNames = 'Subdir2aFile1.groovy'
        def results = analyzer.analyze(ruleSet)
        log("results=$results")

        def fullPaths = results.getViolationsWithPriority(1).collect { it.message }
        assert fullPaths == [
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File1.groovy',
                'src/test/resources/sourcewithdirs/subdir2/Subdir2File1.groovy'
        ]

        assert testCountRule.count == 2
        assert results.numberOfFilesWithViolations == 2
        assert results.totalNumberOfFiles == 2
    }

    void setUp() {
        super.setUp()
        analyzer = new DirectorySourceAnalyzer()
        testCountRule = new TestCountRule()
        ruleSet = new ListRuleSet([new TestPathRule(), testCountRule])
    }

    private List resultsPaths(Results results, List paths=[]) {
        if (results.path) {
            paths << results.path
        }
        results.children.each { child -> resultsPaths(child, paths) }
        return paths
    }

    private List childResultsClasses(Results results) {
        return results.children.collect { it.getClass() }
    }

    private String path(String path) {
        return path.replaceAll('\\\\', '/')
    }
}