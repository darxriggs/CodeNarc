/*
 * Copyright 2018 the original author or authors.
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
package org.codenarc.rule.formatting

import org.junit.Test
import org.codenarc.rule.AbstractRuleTestCase

/**
 * Tests for ClassEndsWithBlankLineRule
 *
 * @author David Ausín
 */
class ClassEndsWithBlankLineRuleTest extends AbstractRuleTestCase<ClassEndsWithBlankLineRule> {

    @Test
    void testRuleProperties() {
        assert rule.priority == 2
        assert rule.name == 'ClassEndsWithBlankLine'
        assert rule.ignoreSingleLineClasses == true
        assert rule.blankLineRequired == true
    }

    @Test
    void testViolationsWithSingleClassWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }

            }
        '''

        rule.blankLineRequired = true

        assertNoViolations(SOURCE)
    }

    @Test
    void testViolationsWithInterfaceClassWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            interface Foo {
 
                void hi()

            }
        '''

        rule.blankLineRequired = true

        assertNoViolations(SOURCE)
    }

    @Test
    void testViolationsWithSingleClassWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }

            }
        '''

        rule.blankLineRequired = false

        assertSingleViolation(SOURCE, 8, '')
    }

    @Test
    void testViolationsWithInterfaceWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            interface Foo {
                
                void hi() 

            }
        '''

        rule.blankLineRequired = false

        assertSingleViolation(SOURCE, 6, '')
    }

    @Test
    void testViolationsWithSingleClassWhenBraceIsNotInANewLineAndClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
        class Foo {
            int a
            
            void hi() {

            }        }
        '''

        rule.blankLineRequired = true

        assertSingleViolation(SOURCE, 7, '            }        }')
    }

    @Test
    void testViolationsWithInterfaceWhenBraceIsNotInANewLineAndClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
        interface Foo {
            
            void hi()
         }
        '''

        rule.blankLineRequired = true

        assertSingleViolation(SOURCE, 5, '}')
    }

    @Test
    void testViolationsWithSingleClassWhenBraceIsNotInANewLineAndClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
        class Foo {
            int a
            
            void hi() {

            }        }
        '''

        rule.blankLineRequired = false

        assertNoViolations(SOURCE)
    }

    @Test
    void testViolationWithSingleClassWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }

            }
        '''
        rule.blankLineRequired = false

        assertSingleViolation(SOURCE, 8, '            }')
    }

    @Test
    void testNoViolationsWithSeveralClassesWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }

            }
            interface Bar {
                
                void hi()
                        
            }
        '''
        rule.blankLineRequired = true

        assertNoViolations(SOURCE)
    }

    @Test
    void testViolationsWithSeveralClassesWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }

            }
            class Bar {
                int a
                
                void hi() {
                }

            }
        '''
        rule.blankLineRequired = false

        assertViolations(SOURCE,
                [lineNumber: 8, sourceLineText: '            }', messageText: 'Class ends with an empty line before the closing brace'],
                [lineNumber: 15, sourceLineText: '            }', messageText: 'Class ends with an empty line before the closing brace'])
    }

    @Test
    void testViolationWithSeveralClassesWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }   
            }
            
            class Bar {
                int a
                
                void hi() {
                }
            }
        '''
        rule.blankLineRequired = true

        assertViolations(SOURCE,
                [lineNumber    : 7, sourceLineText: '            }', messageText   : 'Class does not end with a blank line before the closing brace'],
                [lineNumber    : 14, sourceLineText: '            }', messageText   : 'Class does not end with a blank line before the closing brace'])
    }

    @Test
    void testNoViolationWithSeveralClassesWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }   
            }
            
            class Bar {
                int a
                
                void hi() {
                }
            }
        '''
        rule.blankLineRequired = false

        assertNoViolations(SOURCE)
    }

    @Test
    void testNoViolationsWithNonStaticInnerClassesWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }
                class Bar {
                    int a
                
                    void hi() {
                    }

                }

            }
        '''
        rule.blankLineRequired = true

        assertNoViolations(SOURCE)
    }

    @Test
    void testViolationsWithNonStaticInnerClassesWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }
                class Bar {
                    int a
                
                    void hi() {
                    }

                }

            }
        '''
        rule.blankLineRequired = false

        assertViolations(SOURCE,
                [lineNumber: 13, sourceLineText: '            }', messageText: 'Class ends with an empty line before the closing brace'],
                [lineNumber: 15, sourceLineText: '            }', messageText: 'Class ends with an empty line before the closing brace'])
    }

    @Test
    void testViolationsWithNonStaticInnerClassesWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }
                
                class Bar {
                    int a
                
                    void hi() {
                    }
                }
            }
        '''
        rule.blankLineRequired = true

        assertViolations(SOURCE,
                [lineNumber: 13, sourceLineText: '            }', messageText: 'Class does not end with a blank line before the closing brace'],
                [lineNumber: 14, sourceLineText: '            }', messageText: 'Class does not end with a blank line before the closing brace'])
    }

    @Test
    void testNoViolationsWithStaticInnerClassesWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }
                static class Bar {
                    int a
                
                    void hi() {
                    }

                }

            }
        '''
        rule.blankLineRequired = true

        assertNoViolations(SOURCE)
    }

    @Test
    void testNoViolationsWithStaticInnerClassesWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }
                static class Bar {
                    int a
                
                    void hi() {
                    }
                }
            }
        '''
        rule.blankLineRequired = false

        assertNoViolations(SOURCE)
    }

    @Test
    void testViolationsWithStaticInnerClassesWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }
                
                static class Bar {
                    int a
                
                    void hi() {
                    }
                }
            }
        '''
        rule.blankLineRequired = true

        assertViolations(SOURCE,
                [lineNumber: 13, sourceLineText: '            }', messageText: 'Class does not end with a blank line before the closing brace'],
                [lineNumber: 14, sourceLineText: '            }', messageText: 'Class does not end with a blank line before the closing brace'])
    }

    @Test
    void testViolationsWithStaticInnerClassesWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo {
                int a
                
                void hi() {
                }
                
                static class Bar {
                    int a
                
                    void hi() {
                    }
                    
                }
                
            }
        '''
        rule.blankLineRequired = false

        assertViolations(SOURCE,
                [lineNumber: 14, sourceLineText: '            }', messageText: 'Class ends with an empty line before the closing brace'],
                [lineNumber: 16, sourceLineText: '            }', messageText: 'Class ends with an empty line before the closing brace'])
    }

    @Test
    void testNoViolationsWithSingleLineClassesIgnoredWhenBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            import my.company.Bar
            class Foo extends Bar<String> { }
            
            class Doe extends Bar<String> { }
        '''
        rule.blankLineRequired = true
        rule.ignoreSingleLineClasses = true

        assertNoViolations(SOURCE)
    }

    @Test
    void testNoViolationsWithSingleLineClassesIgnoredWhenBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            import my.company.Bar
            class Foo extends Bar<String> { }
            
            class Doe extends Bar<String> { }
        '''
        rule.blankLineRequired = false
        rule.ignoreSingleLineClasses = true

        assertNoViolations(SOURCE)
    }

    @Test
    void testViolationsWithSingleLineClassesNotAllowedWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            import my.company.Bar
            class Foo extends Bar<String> { }

            class Doe extends Bar<String> { }
            abstract class John  { abstract void a() }
        '''

        rule.ignoreSingleLineClasses = false
        rule.blankLineRequired = true

        assertViolations(SOURCE,
                [lineNumber    : 3, sourceLineText: 'class Foo extends Bar<String> { }', messageText   : 'Single line classes are not allowed'],
                [lineNumber    : 5, sourceLineText: 'class Doe extends Bar<String> { }', messageText   : 'Single line classes are not allowed'],
                [lineNumber    : 6, sourceLineText: 'abstract class John  { abstract void a() }', messageText   : 'Single line classes are not allowed'])
    }

    @Test
    void testNoViolationsWithSingleLineClassesNotAllowedWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            import my.company.Bar
            class Foo extends Bar<String> { }

            class Doe extends Bar<String> { }
            abstract class John  { abstract void a() }
        '''

        rule.ignoreSingleLineClasses = false
        rule.blankLineRequired = false

        assertNoViolations(SOURCE)
    }

    @Test
    void testNoViolationsWithAnonymousClassesWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo { 
                Bar a = new Bar() {
                    
                    @Override
                    String toString() {
                        "Hello world"
                    }

                }

            }            
        '''
        rule.blankLineRequired = true

        assertNoViolations(SOURCE)
    }

    @Test
    void testNoViolationsWithAnonymousClassesWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo { 
                Bar a = new Bar() {
                    
                    @Override
                    String toString() {
                        "Hello world"
                    }
                }
            }            
        '''
        rule.blankLineRequired = false

        assertNoViolations(SOURCE)
    }

    @Test
    void testViolationsWithAnonymousClassesWhenClassEndsWithBlankLineIsRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo { 
                Bar a = new Bar() {
                    
                    @Override
                    String toString() {
                        "Hello world"
                    }
                }

            }            
        '''
        rule.blankLineRequired = true

        assertSingleViolation(SOURCE, 9, '                }')
    }

    @Test
    void testViolationsWithAnonymousClassesWhenClassEndsWithBlankLineIsNotRequired() {
        @SuppressWarnings('TrailingWhitespace')
        final String SOURCE = '''
            class Foo { 
                Bar a = new Bar() {
                    
                    @Override
                    String toString() {
                        "Hello world"
                    }
                    
                }
            }            
        '''
        rule.blankLineRequired = false
        assertSingleViolation(SOURCE, 10, '                }')
    }

    @Override
    protected ClassEndsWithBlankLineRule createRule() {
        new ClassEndsWithBlankLineRule()
    }
}
