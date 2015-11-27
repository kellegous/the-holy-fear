require 'erb'

def set_gopath(paths)
  ENV['GOPATH'] = paths.map { |p|
    "#{Dir.pwd}/#{p}"
  }.join(':')
end

# generates build rules for protobufs. Rules that target dst are generated
# by scanning src.
def protoc(src, dst)
	file dst do
		FileUtils.mkdir_p dst
	end

	FileList["#{src}/*.proto"].map do |proto|
		dest = File.join(dst, File.basename(proto, '.proto') + '.pb.go')
		file dest => [proto, dst] do
			sh 'protoc', "-I#{src}", "--go_out=#{dst}", proto
		end
		dest
	end
end

def go_get(dst, deps)
	deps.map do |pkg|
		path = pkg.gsub(/\/\.\.\.$/, '')
		dest = File.join(dst, 'src', path)
		file dest do
			sh 'go', 'get', pkg
		end
		dest
	end
end

class Path
  def <<(p)
    path = ENV['PATH']
    p = File.join(Dir.pwd, p)
    ENV['PATH'] = "#{p}:#{path}"
  end
end

def path
  Path.new
end

def deps(lst)
  lst.flatten
end

def erb_to(src, dst, ctx)
  val = File.open(src, 'r') do |r|
    r.read
  end
  File.open(dst, 'w') do |w|
    w.puts ERB.new(val).result(ctx)
  end
end
