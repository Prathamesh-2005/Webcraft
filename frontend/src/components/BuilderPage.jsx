import React, { useState, useEffect, useCallback, useRef } from 'react';
import Editor from '@monaco-editor/react';
import axios from 'axios';
import { Link } from 'react-router-dom';

const BuilderPage = () => {
  const [prompt, setPrompt] = useState('');
  const [htmlCode, setHtmlCode] = useState('');
  const [cssCode, setCssCode] = useState('');
  const [jsCode, setJsCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('html');
  const [previewKey, setPreviewKey] = useState(0);
  const [showFullscreen, setShowFullscreen] = useState(false);
  const [error, setError] = useState('');
  const [generationStage, setGenerationStage] = useState('');

  // Netlify deployment states
  const [deploying, setDeploying] = useState(false);
  const [deploymentUrl, setDeploymentUrl] = useState('');
  const [deploymentHistory, setDeploymentHistory] = useState([]);
  const [showDeploymentModal, setShowDeploymentModal] = useState(false);

  // Voice input states
  const [isListening, setIsListening] = useState(false);
  const [voiceSupported, setVoiceSupported] = useState(false);
  const recognitionRef = useRef(null);
  const iframeRef = useRef(null);
  const fullscreenIframeRef = useRef(null);

  // Initialize speech recognition
  useEffect(() => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      setVoiceSupported(true);
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      recognitionRef.current = new SpeechRecognition();

      recognitionRef.current.continuous = false;
      recognitionRef.current.interimResults = false;
      recognitionRef.current.lang = 'en-US';

      recognitionRef.current.onstart = () => {
        setIsListening(true);
      };

      recognitionRef.current.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        setPrompt(prev => prev + (prev ? ' ' + transcript : transcript));
      };

      recognitionRef.current.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        setError(`Voice recognition error: ${event.error}`);
        setIsListening(false);
      };

      recognitionRef.current.onend = () => {
        setIsListening(false);
      };
    }
  }, []);

  // Voice input functions
  const startListening = () => {
    if (recognitionRef.current && voiceSupported) {
      setError('');
      recognitionRef.current.start();
    }
  };

  const stopListening = () => {
    if (recognitionRef.current && isListening) {
      recognitionRef.current.stop();
    }
  };

  // Download functions
  const downloadFile = (content, filename, contentType) => {
    const blob = new Blob([content], { type: contentType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const downloadHTML = () => {
    if (!htmlCode.trim()) {
      setError('No HTML code to download');
      return;
    }
    downloadFile(htmlCode, 'index.html', 'text/html');
  };

  const downloadCSS = () => {
    if (!cssCode.trim()) {
      setError('No CSS code to download');
      return;
    }
    downloadFile(cssCode, 'styles.css', 'text/css');
  };

  const downloadJS = () => {
    if (!jsCode.trim()) {
      setError('No JavaScript code to download');
      return;
    }
    downloadFile(jsCode, 'script.js', 'text/javascript');
  };

  const downloadAll = () => {
    if (!htmlCode.trim() && !cssCode.trim() && !jsCode.trim()) {
      setError('No code to download');
      return;
    }

    // Create a complete HTML file with embedded CSS and JS
    const completeHTML = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Generated Website</title>
    <style>
        ${cssCode}
    </style>
</head>
<body>
    ${htmlCode}
    
    <script>
        ${jsCode}
    </script>
</body>
</html>`;

    downloadFile(completeHTML, 'complete-website.html', 'text/html');

    // Also download individual files if they exist
    if (htmlCode.trim()) downloadFile(htmlCode, 'index.html', 'text/html');
    if (cssCode.trim()) downloadFile(cssCode, 'styles.css', 'text/css');
    if (jsCode.trim()) downloadFile(jsCode, 'script.js', 'text/javascript');
  };

  // Generation stages for better UX
  const generationStages = [
    "Analyzing your request...",
    "Generating HTML structure...",
    "Crafting beautiful CSS styles...",
    "Adding interactive JavaScript...",
    "Optimizing and finalizing...",
    "Almost ready..."
  ];

  const deploymentStages = [
    "Preparing files for deployment...",
    "Uploading to Netlify...",
    "Building your website...",
    "Deploying to production...",
    "Finalizing deployment...",
    "Getting live URL..."
  ];

  const handleGenerate = async () => {
    if (!prompt.trim()) return;

    setLoading(true);
    setError('');
    setGenerationStage(generationStages[0]);

    // Simulate progress stages
    const stageInterval = setInterval(() => {
      setGenerationStage(prev => {
        const currentIndex = generationStages.indexOf(prev);
        if (currentIndex < generationStages.length - 1) {
          return generationStages[currentIndex + 1];
        }
        return prev;
      });
    }, 1500);

    try {
      const res = await axios.post('http://localhost:8080/generate', { prompt });

      // Validate the response
      if (!res.data || (!res.data.html && !res.data.css && !res.data.js)) {
        throw new Error('Invalid response from server');
      }

      setHtmlCode(res.data.html || '');
      setCssCode(res.data.css || '');
      setJsCode(res.data.js || '');
      setPreviewKey(prev => prev + 1);

    } catch (err) {
      console.error('Generation error:', err);
      setError(err.response?.data?.message || err.message || 'Error generating code');
    } finally {
      clearInterval(stageInterval);
      setLoading(false);
      setGenerationStage('');
    }
  };

  const handleDeploy = async () => {
    if (!htmlCode.trim() && !cssCode.trim() && !jsCode.trim()) {
      setError('No code to deploy. Please generate a website first.');
      return;
    }

    setDeploying(true);
    setError('');
    setGenerationStage(deploymentStages[0]);

    // Simulate deployment progress
    const stageInterval = setInterval(() => {
      setGenerationStage(prev => {
        const currentIndex = deploymentStages.indexOf(prev);
        if (currentIndex < deploymentStages.length - 1) {
          return deploymentStages[currentIndex + 1];
        }
        return prev;
      });
    }, 1500);

    try {
      const projectName = `webcraft-${Date.now()}`;
      const res = await axios.post('http://localhost:8080/deploy', {
        html: htmlCode,
        css: cssCode,
        js: jsCode,
        projectName: projectName
      });

      if (res.data.deployed && res.data.deploymentUrl) {
        setDeploymentUrl(res.data.deploymentUrl);

        // Add to deployment history
        const newDeployment = {
          id: Date.now(),
          url: res.data.deploymentUrl,
          projectName: res.data.projectName,
          timestamp: new Date().toISOString(),
          prompt: 'Website deployment'
        };

        setDeploymentHistory(prev => [newDeployment, ...prev.slice(0, 4)]);
        setShowDeploymentModal(true);
      }

    } catch (err) {
      console.error('Deployment error:', err);
      setError(err.response?.data?.message || err.message || 'Error deploying website');
    } finally {
      clearInterval(stageInterval);
      setDeploying(false);
      setGenerationStage('');
    }
  };

  // Sanitize and clean the generated code
  const sanitizeCode = useCallback((code, type) => {
    if (!code) return '';

    // Remove any script tags that try to load external resources
    if (type === 'html') {
      return code.replace(/<script[^>]*src[^>]*><\/script>/gi, '')
        .replace(/<link[^>]*rel=["']stylesheet["'][^>]*>/gi, (match) => {
          // Only allow inline styles, remove external CSS links
          return match.includes('href') ? '' : match;
        })
        // Prevent navigation by replacing href attributes
        .replace(/href=["'][^"']*["']/gi, 'href="#"')
        // Add click prevention to all links
        .replace(/<a\s/gi, '<a onclick="event.preventDefault(); return false;" ');
    }

    // Clean JavaScript
    if (type === 'js') {
      // Remove any import/require statements that might cause issues
      return code.replace(/import\s+.*?from\s+['"][^'"]*['"];?\s*/gi, '')
        .replace(/require\s*\(['"][^'"]*['"]\);?\s*/gi, '')
        .replace(/console\.log\s*\(/g, '// console.log('); // Comment out console.logs
    }

    return code;
  }, []);

  // Handle iframe messages to prevent navigation issues
  useEffect(() => {
    const handleMessage = (event) => {
      // Handle any iframe messages if needed
      if (event.data && event.data.type === 'navigation') {
        event.preventDefault();
        console.log('Prevented navigation in preview');
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  const getPreviewSrc = useCallback(() => {
    const sanitizedHtml = sanitizeCode(htmlCode, 'html');
    const sanitizedCss = sanitizeCode(cssCode, 'css');
    const sanitizedJs = sanitizeCode(jsCode, 'js');

    return `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Preview</title>
    <style>
        /* Reset and base styles */
        * { box-sizing: border-box; }
        body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif; }
        
        /* Prevent navigation styles */
        a { cursor: pointer !important; }
        
        /* User CSS */
        ${sanitizedCss}
    </style>
</head>
<body>
    ${sanitizedHtml}
    
    <script>
        // Error handling wrapper
        window.onerror = function(msg, url, lineNo, columnNo, error) {
            console.error('Preview Error:', msg, 'at line', lineNo);
            return false;
        };
        
        // Prevent all navigation attempts
        document.addEventListener('DOMContentLoaded', function() {
            // Prevent all form submissions
            document.addEventListener('submit', function(e) {
                e.preventDefault();
                console.log('Form submission prevented in preview mode');
            });
            
            // Prevent all link clicks that would navigate
            document.addEventListener('click', function(e) {
                const target = e.target.closest('a');
                if (target && target.href && target.href !== '#' && !target.href.startsWith('javascript:')) {
                    e.preventDefault();
                    console.log('Link navigation prevented in preview mode');
                }
            });
            
            // Override window.open
            window.open = function() {
                console.log('window.open prevented in preview mode');
                return null;
            };
            
            // Override location changes
            const originalReplace = window.location.replace;
            const originalAssign = window.location.assign;
            window.location.replace = function() {
                console.log('location.replace prevented in preview mode');
            };
            window.location.assign = function() {
                console.log('location.assign prevented in preview mode');
            };
        });
        
        // Wrap user JavaScript in try-catch
        try {
            ${sanitizedJs}
        } catch (error) {
            console.error('JavaScript execution error:', error);
        }
    </script>
</body>
</html>`;
  }, [htmlCode, cssCode, jsCode, sanitizeCode]);

  useEffect(() => {
    if (htmlCode || cssCode || jsCode) {
      const timer = setTimeout(() => {
        const refreshButton = document.querySelector('.preview-refresh');
        if (refreshButton) {
          refreshButton.classList.add('animate-ping-once');
          setTimeout(() => {
            refreshButton.classList.remove('animate-ping-once');
          }, 500);
        }
      }, 300);
      return () => clearTimeout(timer);
    }
  }, [htmlCode, cssCode, jsCode]);

  const handleEditorChange = useCallback((value, language) => {
    if (language === 'html') setHtmlCode(value || '');
    if (language === 'css') setCssCode(value || '');
    if (language === 'javascript') setJsCode(value || '');
  }, []);

  const resetAll = useCallback(() => {
    setHtmlCode('');
    setCssCode('');
    setJsCode('');
    setPrompt('');
    setError('');
    setDeploymentUrl('');
    setPreviewKey(prev => prev + 1);
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 to-gray-800 text-white">
      {/* Deployment Success Modal */}
      {showDeploymentModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl p-6 max-w-md w-full border border-gray-700 shadow-2xl">
            <div className="text-center">
              <div className="bg-green-500/20 p-3 rounded-full inline-block mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>

              <h3 className="text-xl font-bold text-white mb-2">🎉 Deployment Successful!</h3>
              <p className="text-gray-300 mb-4">Your website is now live on Netlify</p>

              <div className="bg-gray-800/50 p-3 rounded-lg mb-4">
                <p className="text-sm text-gray-400 mb-1">Live URL:</p>
                <a
                  href={deploymentUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-400 hover:text-blue-300 underline break-all"
                >
                  {deploymentUrl}
                </a>
              </div>

              <div className="flex space-x-3">
                <button
                  onClick={() => setShowDeploymentModal(false)}
                  className="flex-1 bg-gray-700 hover:bg-gray-600 py-2 px-4 rounded-lg transition-colors"
                >
                  Close
                </button>
                <a
                  href={deploymentUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex-1 bg-blue-600 hover:bg-blue-700 py-2 px-4 rounded-lg transition-colors text-center"
                >
                  Visit Site
                </a>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Fullscreen Modal */}
      {showFullscreen && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-lg flex flex-col">
          <div className="flex justify-between items-center p-4 bg-gray-900/80 border-b border-gray-700">
            <h2 className="text-xl font-semibold flex items-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M3 5a2 2 0 012-2h10a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V5zm11 1H6v8l4-2 4 2V6z" clipRule="evenodd" />
              </svg>
              Fullscreen Preview
            </h2>
            <button
              className="bg-gray-700 hover:bg-gray-600 p-2 rounded-full transition-colors"
              onClick={() => setShowFullscreen(false)}
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
              </svg>
            </button>
          </div>
          <iframe
            ref={fullscreenIframeRef}
            title="Fullscreen Preview"
            srcDoc={getPreviewSrc()}
            className="w-full h-full"
            sandbox="allow-scripts"
            style={{ border: 'none' }}
          />
        </div>
      )}

      {/* Header */}
      <header className="sticky top-0 z-40 backdrop-blur-md bg-gray-900/80 border-b border-gray-700 shadow-xl">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <Link to="/" className="flex items-center space-x-3">
              <div className="bg-gradient-to-r from-blue-500 to-purple-600 p-2 rounded-lg">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8" viewBox="0 0 24 24" fill="none">
                  <path d="M10 20L14 4M18 8L22 12L18 16M6 16L2 12L6 8" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </div>
              <h1 className="text-3xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-blue-300 via-purple-300 to-indigo-300">
                WebCraft
              </h1>
            </Link>

            <div className="flex items-center space-x-4">
              {/* Download Dropdown */}
              <div className="relative group">
                <button className="px-6 py-3 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 rounded-xl shadow-lg font-medium flex items-center space-x-2 transition-all duration-300 transform hover:scale-105">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                  <span>Download</span>
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                </button>

                <div className="absolute right-0 mt-2 w-48 bg-gray-800 rounded-xl shadow-2xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 z-50 border border-gray-700">
                  <div className="py-2">
                    <button
                      onClick={downloadAll}
                      className="w-full text-left px-4 py-3 text-sm hover:bg-gradient-to-r hover:from-gray-700 hover:to-gray-600 transition-all flex items-center space-x-3"
                    >
                      <div className="bg-gradient-to-r from-green-500 to-emerald-500 p-1.5 rounded-lg">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-white" viewBox="0 0 20 20" fill="currentColor">
                          <path d="M4 4a2 2 0 00-2 2v8a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2H4zm0 2h12v8H4V6z" />
                        </svg>
                      </div>
                      <span className="font-medium">Download All Files</span>
                    </button>
                    <hr className="my-1 border-gray-600" />
                    <button
                      onClick={downloadHTML}
                      className="w-full text-left px-4 py-2 text-sm hover:bg-gray-700 transition-colors flex items-center space-x-3"
                    >
                      <span className="text-orange-400 font-semibold text-xs bg-orange-400/10 px-2 py-1 rounded">HTML</span>
                      <span>index.html</span>
                    </button>
                    <button
                      onClick={downloadCSS}
                      className="w-full text-left px-4 py-2 text-sm hover:bg-gray-700 transition-colors flex items-center space-x-3"
                    >
                      <span className="text-blue-400 font-semibold text-xs bg-blue-400/10 px-2 py-1 rounded">CSS</span>
                      <span>styles.css</span>
                    </button>
                    <button
                      onClick={downloadJS}
                      className="w-full text-left px-4 py-2 text-sm hover:bg-gray-700 transition-colors flex items-center space-x-3"
                    >
                      <span className="text-yellow-400 font-semibold text-xs bg-yellow-400/10 px-2 py-1 rounded">JS</span>
                      <span>script.js</span>
                    </button>
                  </div>
                </div>
              </div>

              {/* Generate Button */}
              <button
                className={`px-6 py-3 rounded-xl shadow-lg font-medium flex items-center space-x-2 transition-all duration-300 transform hover:scale-105
                  ${loading
                    ? 'bg-gray-700 cursor-not-allowed'
                    : 'bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700'
                  }`}
                onClick={handleGenerate}
                disabled={loading || deploying || !prompt.trim()}
              >
                {loading ? (
                  <>
                    <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span>Generating...</span>
                  </>
                ) : (
                  <>
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-11a1 1 0 10-2 0v2H7a1 1 0 100 2h2v2a1 1 0 102 0v-2h2a1 1 0 100-2h-2V7z" clipRule="evenodd" />
                    </svg>
                    <span>Generate Website</span>
                  </>
                )}
              </button>

              {/* Deploy Button */}
              <button
                className={`px-6 py-3 rounded-xl shadow-lg font-medium flex items-center space-x-2 transition-all duration-300 transform hover:scale-105
                  ${deploying
                    ? 'bg-gray-700 cursor-not-allowed'
                    : 'bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700'
                  }`}
                onClick={handleDeploy}
                disabled={loading || deploying || (!htmlCode && !cssCode && !jsCode)}
              >
                {deploying ? (
                  <>
                    <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span>Deploying...</span>
                  </>
                ) : (
                  <>
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                    <span>Deploy</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Loading/Deployment Status */}
      {(loading || deploying) && generationStage && (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4">
          <div className={`border px-6 py-4 rounded-xl flex items-center backdrop-blur-sm ${deploying
              ? 'bg-gradient-to-r from-purple-900/50 to-pink-900/50 border-purple-600/30 text-purple-200'
              : 'bg-gradient-to-r from-blue-900/50 to-purple-900/50 border-blue-600/30 text-blue-200'
            }`}>
            <div className="flex items-center space-x-3">
              <div className="flex space-x-1">
                <div className={`w-2 h-2 rounded-full animate-bounce ${deploying ? 'bg-purple-400' : 'bg-blue-400'}`}></div>
                <div className={`w-2 h-2 rounded-full animate-bounce ${deploying ? 'bg-purple-400' : 'bg-blue-400'}`} style={{ animationDelay: '0.1s' }}></div>
                <div className={`w-2 h-2 rounded-full animate-bounce ${deploying ? 'bg-purple-400' : 'bg-blue-400'}`} style={{ animationDelay: '0.2s' }}></div>
              </div>
              <span className="font-medium">{generationStage}</span>
            </div>
          </div>
        </div>
      )}

      {/* Current Deployment URL Display */}
      {deploymentUrl && (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4">
          <div className="bg-gradient-to-r from-green-900/50 to-emerald-900/50 border border-green-600/30 text-green-200 px-6 py-4 rounded-xl flex items-center justify-between backdrop-blur-sm">
            <div className="flex items-center space-x-3">
              <div className="bg-green-500/20 p-2 rounded-lg">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <div>
                <p className="font-medium">🎉 Your website is live!</p>
                <a
                  href={deploymentUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-green-300 hover:text-green-200 underline text-sm break-all"
                >
                  {deploymentUrl}
                </a>
              </div>
            </div>
            <a
              href={deploymentUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="bg-green-600 hover:bg-green-700 px-4 py-2 rounded-lg transition-colors flex items-center space-x-2"
            >
              <span>Visit Site</span>
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          </div>
        </div>
      )}

      {/* Error Display */}
      {error && (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4">
          <div className="bg-red-900/50 border border-red-600 text-red-200 px-4 py-3 rounded-lg flex items-center">
            <svg className="h-5 w-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            {error}
          </div>
        </div>
      )}

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Preview Section */}
          <div className="space-y-4">
            <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl p-1 shadow-2xl">
              <div className="bg-gray-900/50 rounded-xl p-5">
                <div className="flex justify-between items-center mb-4">
                  <h2 className="text-xl font-semibold flex items-center">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M3 5a2 2 0 012-2h10a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V5zm11 1H6v8l4-2 4 2V6z" clipRule="evenodd" />
                    </svg>
                    Live Preview
                  </h2>
                  <div className="flex items-center space-x-2">
                    <span className="flex h-3 w-3">
                      <span className="animate-ping absolute inline-flex h-3 w-3 rounded-full bg-green-400 opacity-75"></span>
                      <span className="relative inline-flex rounded-full h-3 w-3 bg-green-500"></span>
                    </span>
                    <span className="text-sm text-green-400">Safe Mode</span>
                  </div>
                </div>

                <div className="relative bg-gray-900 rounded-lg overflow-hidden shadow-xl h-[500px] md:h-[700px]">
                  {htmlCode || cssCode || jsCode ? (
                    <iframe
                      ref={iframeRef}
                      key={previewKey}
                      title="Live Preview"
                      srcDoc={getPreviewSrc()}
                      className="w-full h-full rounded-lg relative z-10"
                      sandbox="allow-scripts"
                      style={{ border: 'none' }}
                    />
                  ) : (
                    <div className="absolute inset-0 flex items-center justify-center bg-gray-900 z-0">
                      <div className="text-center">
                        <div className="bg-gray-800/50 rounded-full p-6 inline-block mb-6">
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                          </svg>
                        </div>
                        <h3 className="text-xl font-medium text-gray-300 mb-2">Preview Unavailable</h3>
                        <p className="text-gray-500 max-w-md">
                          Generate a website to see the live preview here
                        </p>
                      </div>
                    </div>
                  )}

                  {/* Control Buttons */}
                  <div className="absolute bottom-4 right-4 z-20 flex space-x-2">
                    <button
                      className="preview-refresh bg-gray-800/80 backdrop-blur-sm p-2 rounded-full hover:bg-gray-700 transition-all"
                      onClick={() => setPreviewKey(prev => prev + 1)}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd" />
                      </svg>
                    </button>

                    <button
                      className="bg-gray-800/80 backdrop-blur-sm p-2 rounded-full hover:bg-gray-700 transition-all"
                      onClick={() => setShowFullscreen(true)}
                      disabled={!htmlCode && !cssCode && !jsCode}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M3 4a1 1 0 011-1h4a1 1 0 010 2H6.414l2.293 2.293a1 1 0 11-1.414 1.414L5 6.414V8a1 1 0 01-2 0V4zm9 1a1 1 0 110-2h4a1 1 0 011 1v4a1 1 0 11-2 0V6.414l-2.293 2.293a1 1 0 11-1.414-1.414L13.586 5H12zm-9 9a1 1 0 012 0v1.586l2.293-2.293a1 1 0 111.414 1.414L6.414 15H8a1 1 0 110 2H4a1 1 0 01-1-1v-4zm13-1a1 1 0 011 1v1.586l1.293-1.293a1 1 0 111.414 1.414L16.414 15H15a1 1 0 110-2h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 110-2h1.586l-1.293-1.293a1 1 0 111.414-1.414l1.293 1.293V13a1 1 0 011-1z" clipRule="evenodd" />
                      </svg>
                    </button>
                  </div>
                  <div className="absolute inset-0 bg-gradient-to-t from-gray-900/70 to-transparent pointer-events-none z-10"></div>
                </div>
              </div>
            </div>
          </div>

          {/* Controls Section */}
          <div className="space-y-6">
            {/* Prompt Input with Voice */}
            <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl p-1 shadow-xl">
              <div className="bg-gray-900/50 rounded-xl p-5 h-full">
                <div className="flex justify-between items-center mb-3">
                  <h2 className="text-lg font-semibold flex items-center">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2 text-purple-400" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                    </svg>
                    Describe Your Website
                  </h2>

                  {/* Voice Input Button */}
                  {voiceSupported && (
                    <button
                      onClick={isListening ? stopListening : startListening}
                      disabled={loading || deploying}
                      className={`p-2 rounded-full transition-all duration-300 flex items-center space-x-2 
                        ${isListening
                          ? 'bg-red-600 hover:bg-red-700 animate-pulse'
                          : 'bg-purple-600 hover:bg-purple-700'
                        }`}
                      title={isListening ? 'Stop listening' : 'Start voice input'}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M7 4a3 3 0 016 0v4a3 3 0 11-6 0V4zm4 10.93A7.001 7.001 0 0017 8a1 1 0 10-2 0A5 5 0 015 8a1 1 0 10-2 0 7.001 7.001 0 006 6.93V17H6a1 1 0 100 2h8a1 1 0 100-2h-3v-2.07z" clipRule="evenodd" />
                      </svg>
                    </button>
                  )}
                </div>

                <div className="relative">
                  <textarea
                    className="w-full bg-gray-800/50 rounded-xl p-4 text-gray-200 placeholder-gray-500 
                              focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none transition-all
                              border border-gray-700 hover:border-gray-600 min-h-[120px]"
                    placeholder="Example: 'Create a responsive landing page for a tech startup with a dark theme...'"
                    value={prompt}
                    onChange={(e) => setPrompt(e.target.value)}
                    disabled={loading || deploying}
                  />

                  {/* Voice indicator */}
                  {isListening && (
                    <div className="absolute top-2 right-2 flex items-center space-x-2 bg-red-600/20 backdrop-blur-sm rounded-lg px-3 py-1">
                      <div className="flex space-x-1">
                        <div className="w-1 h-4 bg-red-400 rounded-full animate-pulse"></div>
                        <div className="w-1 h-3 bg-red-400 rounded-full animate-pulse" style={{ animationDelay: '0.2s' }}></div>
                        <div className="w-1 h-5 bg-red-400 rounded-full animate-pulse" style={{ animationDelay: '0.4s' }}></div>
                      </div>
                      <span className="text-sm text-red-300">Listening...</span>
                    </div>
                  )}
                </div>

                <div className="mt-3 flex justify-between items-center">
                  <div className="text-sm text-gray-400 flex items-center">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                    </svg>
                    Be as descriptive as possible for better results
                  </div>

                  {voiceSupported && (
                    <div className="text-sm text-gray-500 flex items-center">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M7 4a3 3 0 016 0v4a3 3 0 11-6 0V4zm4 10.93A7.001 7.001 0 0017 8a1 1 0 10-2 0A5 5 0 115 8a1 1 0 10-2 0 7.001 7.001 0 006 6.93V17H6a1 1 0 100 2h8a1 1 0 100-2h-3v-2.07z" clipRule="evenodd" />
                      </svg>
                      Voice input available
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Code Editor */}
            <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl p-1 shadow-xl overflow-hidden">
              <div className="bg-gray-900/50 rounded-xl h-full">
                <div className="flex border-b border-gray-700">
                  {[
                    { key: 'html', label: 'HTML', color: 'text-orange-400' },
                    { key: 'css', label: 'CSS', color: 'text-blue-400' },
                    { key: 'js', label: 'JS', color: 'text-yellow-400' }
                  ].map((tab) => (
                    <button
                      key={tab.key}
                      className={`px-6 py-4 flex-1 text-center transition-all duration-300 relative group
                        ${activeTab === tab.key
                          ? 'text-white'
                          : 'text-gray-400 hover:text-gray-200'
                        }`}
                      onClick={() => setActiveTab(tab.key)}
                    >
                      <span className={activeTab === tab.key ? tab.color : ''}>{tab.label}</span>
                      <span className={`absolute bottom-0 left-0 w-full h-0.5 bg-gradient-to-r from-blue-500 to-purple-500 transition-transform duration-300 transform scale-x-0 group-hover:scale-x-100 ${activeTab === tab.key ? 'scale-x-100' : ''}`}></span>
                    </button>
                  ))}
                </div>

                <div className="p-4 h-[400px]">
                  <Editor
                    language={activeTab === 'js' ? 'javascript' : activeTab}
                    value={activeTab === 'html' ? htmlCode : activeTab === 'css' ? cssCode : jsCode}
                    onChange={(val) => handleEditorChange(val, activeTab === 'js' ? 'javascript' : activeTab)}
                    theme="vs-dark"
                    options={{
                      minimap: { enabled: false },
                      fontSize: 14,
                      lineNumbers: 'on',
                      scrollBeyondLastLine: false,
                      automaticLayout: true,
                      readOnly: loading || deploying,
                      wordWrap: 'on',
                      formatOnPaste: true,
                      formatOnType: true
                    }}
                  />
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="grid grid-cols-1 gap-4">
              <button
                className="w-full bg-gradient-to-r from-gray-700 to-gray-800 py-3 rounded-xl shadow-lg 
                  font-medium flex items-center justify-center space-x-2 transition-all duration-300 transform hover:scale-[1.02] 
                  border border-gray-600 hover:border-gray-500"
                onClick={resetAll}
                disabled={loading || deploying}
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                <span>Reset All</span>
              </button>
            </div>

            {/* Deployment History (if any) */}
            {deploymentHistory.length > 0 && (
              <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl p-1 shadow-xl">
                <div className="bg-gray-900/50 rounded-xl p-5">
                  <h3 className="text-lg font-semibold mb-4 flex items-center">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Recent Deployments
                  </h3>
                  <div className="space-y-3 max-h-48 overflow-y-auto">
                    {deploymentHistory.map((deployment) => (
                      <div key={deployment.id} className="bg-gray-800/50 rounded-lg p-3 flex items-center justify-between">
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-medium text-gray-200 truncate">
                            {deployment.prompt}
                          </p>
                          <p className="text-xs text-gray-400">
                            {new Date(deployment.timestamp).toLocaleString()}
                          </p>
                        </div>
                        <a
                          href={deployment.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="ml-3 bg-indigo-600 hover:bg-indigo-700 px-3 py-1 rounded text-xs transition-colors flex items-center space-x-1"
                        >
                          <span>Visit</span>
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                          </svg>
                        </a>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Additional Features Section */}
        <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-gradient-to-br from-blue-900/30 to-blue-800/30 rounded-xl p-6 border border-blue-700/50">
            <div className="bg-blue-500/20 p-3 rounded-lg inline-block mb-4">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-white mb-2">AI-Powered Generation</h3>
            <p className="text-gray-300 text-sm">
              Advanced AI creates complete websites with HTML, CSS, and JavaScript based on your description.
            </p>
          </div>

          <div className="bg-gradient-to-br from-purple-900/30 to-purple-800/30 rounded-xl p-6 border border-purple-700/50">
            <div className="bg-purple-500/20 p-3 rounded-lg inline-block mb-4">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-white mb-2">One-Click Deployment</h3>
            <p className="text-gray-300 text-sm">
              Deploy your websites instantly to Netlify with live URLs. No configuration required.
            </p>
          </div>

          <div className="bg-gradient-to-br from-green-900/30 to-green-800/30 rounded-xl p-6 border border-green-700/50">
            <div className="bg-green-500/20 p-3 rounded-lg inline-block mb-4">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-white mb-2">Live Code Editor</h3>
            <p className="text-gray-300 text-sm">
              Edit your generated code in real-time with Monaco Editor and see changes instantly.
            </p>
          </div>
        </div>

        {/* Tips Section */}
        <div className="mt-12 bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl p-1 shadow-xl">
          <div className="bg-gray-900/50 rounded-xl p-6">
            <h3 className="text-xl font-semibold mb-4 flex items-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
              </svg>
              Pro Tips for Better Results
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <div className="flex items-start space-x-3">
                  <div className="bg-blue-500/20 p-1 rounded-full flex-shrink-0 mt-1">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <div>
                    <h4 className="font-medium text-gray-200">Be Descriptive</h4>
                    <p className="text-sm text-gray-400">Include details about colors, layout, functionality, and target audience.</p>
                  </div>
                </div>

                <div className="flex items-start space-x-3">
                  <div className="bg-blue-500/20 p-1 rounded-full flex-shrink-0 mt-1">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <div>
                    <h4 className="font-medium text-gray-200">Specify Components</h4>
                    <p className="text-sm text-gray-400">Mention specific elements like headers, navigation, forms, or image galleries.</p>
                  </div>
                </div>

                <div className="flex items-start space-x-3">
                  <div className="bg-blue-500/20 p-1 rounded-full flex-shrink-0 mt-1">
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <div>
                    <h4 className="font-medium text-gray-200">Include Interactions</h4>
                    <p className="text-sm text-gray-400">Describe animations, hover effects, or interactive features you want.</p>
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <div className="bg-gray-800/50 rounded-lg p-4">
                  <h4 className="font-medium text-gray-200 mb-2">Example Prompt:</h4>
                  <p className="text-sm text-gray-400 italic">
                    "Create a modern landing page for a tech startup with a dark theme, gradient backgrounds,
                    hero section with call-to-action button, features grid, testimonials carousel, and contact form.
                    Use blue and purple accent colors with smooth animations."
                  </p>
                </div>

                <div className="flex items-center space-x-2 text-sm text-gray-400">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>All generated websites are mobile-responsive</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-gray-900/50 border-t border-gray-700 mt-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="flex flex-col md:flex-row justify-between items-center">
            <div className="flex items-center space-x-3 mb-4 md:mb-0">
              <div className="bg-gradient-to-r from-blue-500 to-purple-600 p-2 rounded-lg">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" viewBox="0 0 24 24" fill="none">
                  <path d="M10 20L14 4M18 8L22 12L18 16M6 16L2 12L6 8" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">WebCraft</h3>
                <p className="text-sm text-gray-400">AI-Powered Web Development</p>
              </div>
            </div>

            <div className="flex items-center space-x-6 text-sm text-gray-400">
              <span className="flex items-center space-x-2">
                <div className="flex h-2 w-2">
                  <div className="animate-ping absolute inline-flex h-2 w-2 rounded-full bg-green-400 opacity-75"></div>
                  <div className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></div>
                </div>
                <span>All systems operational</span>
              </span>
              <span>•</span>
              <span>Powered by AI & Netlify</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default BuilderPage;