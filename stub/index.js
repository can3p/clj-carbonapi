var express = require('express');
var app = express();

var metrics = {
  "test.random": function(time) {
    return Math.random();
  },

  "test.toggle_constant": function(time) {
    return Math.random() > 0.5 ? 1 : -1;
  }
};

function resolve_metrics_(query) {
  if (Array.isArray(query)) {
    return query.reduce(function(acc, val) {
      return acc.concat(resolve_metrics(val));
    }, []);
  }

  var query = new RegExp(query
                         .replace(/\*/g, "[^.]+")
                         .replace(/{([^}]+)}/g, function(match, args) {
                           return "(" + args.replace(/,/g, "|") + ")";
                         })
                        );
  return Object.keys(metrics)
                    .filter(function(metric) {
                      return metric.match(query)
                    });
}

function resolve_metrics(query) {
  var found =  resolve_metrics_(query)
  var s = found.reduce(function(s, v) { s.add(v); return s; }, new Set());
  var unique = [];

  s.forEach(function(v) { unique.push(v); });

  return unique;
}

function build_timings() {
  var till = Math.floor(Date.now() / 1000);
  var now = till - 1000;
  var timings = [];

  while (now < till) {
    timings.push(now);
    now += 50;
  }

  return timings;
}

function generate_metric(timings, metric) {
  return {
    target: metric,
    datapoints: timings.map(function(ts) {
      return [metrics[metric](ts), ts];
    })
  };
}

function build_metrics(req, res) {
  var matching = resolve_metrics(req.query.target);

  if (matching.length) {
    var timings = build_timings();

    res.json(matching.map(generate_metric.bind(null, timings)));
  } else {
    res.sendStatus(404);
  }
}

app.get('/render', function(req, res) {
  if (req.query.format !== 'json') {
      return res.sendStatus(404);
  }

  if (!req.query.target) {
      return res.sendStatus(404);
  }

  return build_metrics(req, res);
});

var server = app.listen(4000, function () {
  var host = server.address().address;
  var port = server.address().port;

  console.log('Example app listening at http://%s:%s', host, port);
});
