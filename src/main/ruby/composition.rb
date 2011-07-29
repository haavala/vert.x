module Composition
  class Composer
    def Composer.compose
      Composer.new
    end

    def initialize
      @java_composer = org.nodex.core.composition.Composer.compose
    end

    # parallel and then can be combined

    COMPLETION_CLASS_SYM = "org.nodex.core.composition.Completion".to_sym

    def when(*completions, &block)
      if block
        completion = Deferred.new(block)
        completions = [completion]
      end
      java_completions = []
      completions.each { |c| java_completions << c._to_java_completion }
      @java_composer.when(java_completions.to_java(COMPLETION_CLASS_SYM))
      self
    end

    def parallel(*completions)
      java_completions = []
      completions.each { |c| java_completions << c._to_java_completion }
      @java_composer.parallel(java_completions.to_java(COMPLETION_CLASS_SYM))
      self
    end

    def then(completion = nil, &block)
      completion = Deferred.new(block) if !completion
      @java_composer.then(completion._to_java_completion)
      self
    end

    def end
      @java_composer.end
    end

    private :initialize
  end

  class Completion

    def Completion.create
      Completion.new(org.nodex.core.composition.Completion.new)
    end

    def Completion.create_from_java_completion(java_completion)
      Completion.new(java_completion)
    end

    def initialize(java_completion)
      @java_completion = java_completion
    end

    def on_complete(proc = nil, &complete_block)
      complete_block = proc if proc
      @java_completion.onComplete(CompleteHandler.new(complete_block))
    end

    def complete
      @java_completion.complete
    end

    def _to_java_completion
      @java_completion
    end

    private :initialize

  end

  class Deferred
    def initialize(proc = nil, &block)
      block = proc if proc
      @java_deffered = org.nodex.core.composition.Deferred.new(CompleteHandler.new(block))
    end

    def _to_java_completion
      @java_deffered
    end

  end

  class CompleteHandler < org.nodex.core.Runnable
    def initialize(callback)
      super()
      @callback = callback
    end

    def run()
      @callback.call
    end

    private :initialize
  end

end