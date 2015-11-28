require 'fileutils'
require './build'

set_gopath(['dst/go', '.'])
GO_DEPS = go_get('dst/go', [
  'github.com/kellegous/bungler',
])

RUN_DEPS = [
  ['org.apache.tika', 'tika-core', '1.11'],
  ['org.apache.tika', 'tika-parsers', '1.11'],
  ['org.apache.pdfbox', 'pdfbox', '1.8.10'],
  ['org.apache.pdfbox', 'jempbox', '1.8.10'],
  ['org.apache.pdfbox', 'fontbox', '1.8.10'],
  ['com.google.guava', 'guava', '18.0'],
  ['edu.stanford.nlp', 'stanford-corenlp', '3.5.2'],
  ['com.fasterxml.jackson.core', 'jackson-core', '2.6.2'],
  ['commons-codec', 'commons-codec', '1.10'],
  ['commons-cli', 'commons-cli', '1.3.1'],
  ['commons-io', 'commons-io', '2.4'],
  ['commons-logging', 'commons-logging', '1.2'],
  ['org.apache.commons', 'commons-lang3', '3.4'],
  ['com.github.spullara.mustache.java', 'compiler', '0.9.1'],
  ['org.eclipse.jetty', 'jetty-server', '9.3.6.v20151106'],
  ['org.eclipse.jetty', 'jetty-util', '9.3.6.v20151106'],
  ['org.eclipse.jetty', 'jetty-io', '9.3.6.v20151106'],
  ['org.eclipse.jetty', 'jetty-http', '9.3.6.v20151106'],
  ['org.eclipse.jetty', 'jetty-servlet', '9.3.6.v20151106'],
  ['javax.servlet', 'javax.servlet-api', '3.1.0'],
]

TST_DEPS = [
  ['junit', 'junit', '4.12'],
]

DEPS = RUN_DEPS + TST_DEPS

JARJAR_VER = "1.4"
JARJAR_URL = "https://jarjar.googlecode.com/files/jarjar-#{JARJAR_VER}.jar"
JARJAR_JAR = "dst/lib/jarjar-#{JARJAR_VER}.jar"
file JARJAR_JAR do
  FileUtils::mkdir_p(File.dirname(JARJAR_JAR))
  sh 'curl', '-o', JARJAR_JAR, JARJAR_URL
end

def bungle(deps)
  deps.map do |org, name, vers|
    dst = File.join('dst/lib', "#{name}-#{vers}.jar")
    sig = "#{org}/#{name}/#{vers}"
    file dst => GO_DEPS do |t|
      sh 'dst/go/bin/bungler', '--dst=dst/lib', '--recursive=false', sig
      FileUtils.touch dst
    end
  end
end

run_jars = bungle(RUN_DEPS)
tst_jars = bungle(TST_DEPS)

POS_MODELS_URL = 'http://nlp.stanford.edu/software/stanford-postagger-2015-04-20.zip'
POS_MODELS_FILE = 'english-bidirectional-distsim.tagger'
POS_MODELS_DEP = "dst/#{File.basename(POS_MODELS_URL, '.zip')}/models/#{POS_MODELS_FILE}"
file POS_MODELS_DEP do
  sh 'curl', '-o', "dst/#{File.basename(POS_MODELS_URL)}", POS_MODELS_URL
  sh 'unzip', "dst/#{File.basename(POS_MODELS_URL)}", '-d', 'dst'
end

file 'build.xml' => run_jars + ['etc/build.erb.xml', 'Rakefile'] do |t|
  jars = run_jars
  erb_to 'etc/build.erb.xml', t.name, binding
end

file 'dst/dep.jar' => ['build.xml', JARJAR_JAR] + run_jars do |t|
  sh 'ant', '-f', 'build.xml',
    "-Ddep.jar=dst/dep.jar",
    "-Dapp.jar=dst/app.jar",
    "-Djarjar.jar=#{JARJAR_JAR}",
    'dep.jar'
end

file 'dst/app.jar' => ['dst/dep.jar'] + FileList['src/**/*'] do |t|
  sh 'ant', '-f', 'build.xml',
    "-Ddep.jar=dst/dep.jar",
    "-Dapp.jar=dst/app.jar",
    "-Djarjar.jar=#{JARJAR_JAR}",
    'app.jar'
end

file 'layout/node_modules' do
  Dir.chdir('layout') {
    sh 'npm', 'install'
  }
end

task :test => tst_jars do
	sh 'go', 'test', 'relig/scan', 'relig/scan/bible'
end

task :atom do
  sh 'atom', '.'
end

task :nuke do
  FileUtils.rm_rf('dst')
end

task :clean do
  ['dst/www', 'dst/app.jar', 'dst/dep.jar', 'dst/classes', 'dst/cache-*'].each { |f|
    FileUtils.rm_rf(f)
  }
end

task :default => [ 'dst/app.jar', POS_MODELS_DEP ]

task :fear => [ 'dst/app.jar', POS_MODELS_DEP, 'layout/node_modules' ] do
  sh 'bin/create-fear', '--dest-dir=dst/www'
  sh 'electron', 'layout', '--dst=dst/www', 'dst/www/print.json'
end
